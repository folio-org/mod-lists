package org.folio.list.services.export;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.list.exception.ExportNotFoundException.exportNotFound;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.list.configuration.ListExportProperties;
import org.folio.list.domain.AsyncProcessStatus;
import org.folio.list.domain.ExportDetails;
import org.folio.list.domain.ListEntity;
import org.folio.list.exception.ExportCancelledException;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListExportRepository;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.rest.SystemUserQueryClient;
import org.folio.list.services.ListActions;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.exception.S3ClientException;
import org.springframework.stereotype.Service;

/**
 * Creates CSV file for the given export details.
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class CsvCreator {

  private final ListExportRepository listExportRepository;
  private final ListContentsRepository contentsRepository;
  private final ListExportProperties exportProperties;
  private final EntityTypeClient entityTypeClient;
  private final FolioS3Client folioS3Client;
  private final SystemUserQueryClient systemUserQueryClient;

  // Minimal s3 part size is 5 MB
  private static final Long MINIMAL_PART_SIZE = 5242880L;
  private static final String IS_DELETED = "_deleted";
  private static final int MAX_RETRIES = 5;
  private static final long INITIAL_BACKOFF = 1000;
  private static final long MAX_BACKOFF = 16000;

  @SneakyThrows
  public ExportLocalStorage createAndUploadCSV(ExportDetails exportDetails, String destinationFileName, String uploadId, List<String> partETags, UUID userId) {
    var localStorage = new ExportLocalStorage(exportDetails.getExportId());
    ListEntity list = exportDetails.getList();
    var idsProvider = new ListIdsProvider(contentsRepository, list);
    EntityType entityType = entityTypeClient.getEntityType(list.getEntityTypeId(), ListActions.EXPORT);

    OutputStream localStorageOutputStream = localStorage.outputStream();
    var csvWriter = new ListCsvWriter(entityType, exportDetails.getFields());
    int batchSize = exportProperties.getBatchSize();
    int batchNumber = 0;
    int partNumber = 1;

    for (List<List<String>> ids = idsProvider.nextBatch(batchSize); !isEmpty(ids); ids = idsProvider.nextBatch(batchSize)) {
      if (batchNumber % 10 == 0) {
        checkIfExportCancelled(list.getId(), exportDetails.getExportId());

        //Skip the first batch since we haven't generated any content yet and do not upload if file size less than 5 mb
        File multiPartFile = new File(localStorage.getAbsolutePath());
        long bytes = FileUtils.sizeOf(multiPartFile);
        if (batchNumber != 0 && bytes > MINIMAL_PART_SIZE) {
          uploadCSVPart(destinationFileName, uploadId, partNumber, localStorage.getAbsolutePath(), partETags, exportDetails);
          localStorage.rotateFile();
          localStorageOutputStream = localStorage.outputStream();
          partNumber++;
        }
      }
      log.info("Export in progress for exportId {}. Fetched a batch of {} IDs.", exportDetails.getExportId(), ids.size());
      ContentsRequest contentsRequest = new ContentsRequest()
        .entityTypeId(list.getEntityTypeId())
        .fields(exportDetails.getFields())
        .ids(ids)
        .localize(true)
        .userId(userId);
      var sortedContents = systemUserQueryClient.getContentsPrivileged(contentsRequest)
        .stream()
        .filter(map -> !Boolean.TRUE.equals(map.get(IS_DELETED)))
        .toList();
      csvWriter.writeCsv(sortedContents, localStorageOutputStream);
      batchNumber++;
    }

    uploadCSVPart(destinationFileName, uploadId, partNumber, localStorage.getAbsolutePath(), partETags, exportDetails);

    return localStorage;
  }

  @SneakyThrows
  private void uploadCSVPart(String destinationFileName, String uploadId, int partNumber, String localStorage,
                             List<String> partETags, ExportDetails exportDetails) {
    int attempt = 0;
    long backoff = INITIAL_BACKOFF;

    while (attempt < MAX_RETRIES) {
      try {
        String partETag = folioS3Client.uploadMultipartPart(destinationFileName, uploadId, partNumber, localStorage);
        partETags.add(partETag);
        log.info("Generated CSV multipart file for exportID {}. Uploading to S3, Part Number {}", exportDetails.getExportId(), partNumber);
        return;
      } catch (S3ClientException e) {
        attempt++;
        if (attempt >= MAX_RETRIES) {
          log.error("Upload failed after {} attempts: {}", attempt, e.getMessage());
          throw e;
        }
        log.info("Upload part failed, retrying attempt {} after backoff...", attempt);
        TimeUnit.MILLISECONDS.sleep(backoff);
        backoff = Math.min(backoff * 2, MAX_BACKOFF);
      }
    }
  }

  private void checkIfExportCancelled(UUID listId, UUID exportId) {
    ExportDetails exportDetails = listExportRepository.findById(exportId)
      .orElseThrow(() -> exportNotFound(listId, exportId, ListActions.EXPORT));
    if (exportDetails.getStatus().equals(AsyncProcessStatus.CANCELLED)) {
      log.info("Export has been cancelled: exportId {}, listId {}", exportId, listId);
      throw new ExportCancelledException(listId, exportId, ListActions.EXPORT);
    }
  }

  private static class ListCsvWriter {

    private final Pair<CsvSchema, CsvSchema> csvSchemas;
    private final ObjectWriter objectWriter;
    private boolean firstBatch;

    private static final Map<String, CsvSchema.ColumnType> COLUMN_TYPE_MAPPER = Map.of(
      "booleanType", CsvSchema.ColumnType.BOOLEAN,
      "numberType", CsvSchema.ColumnType.NUMBER,
      "arrayType", CsvSchema.ColumnType.ARRAY
    );

    private ListCsvWriter(EntityType entityType, List<String> fields) {
      this.csvSchemas = createSchema(entityType, fields);
      this.objectWriter = new CsvMapper().writerFor(List.class);
      this.firstBatch = true;
    }

    @SneakyThrows
    public void writeCsv(List<Map<String, Object>> listContents, OutputStream destination) {
      if (firstBatch) {
        objectWriter.with(csvSchemas.getRight().withHeader()).writeValues(destination).write(List.of());
      }
      objectWriter.with(csvSchemas.getLeft().withoutHeader()).writeValues(destination).write(listContents);
      firstBatch = false;
      destination.flush();
    }

    /**
     * Creates schemas for the exported CSV. However, we want to use the label alias for the header row,
     * which Jackson does not natively support. In some cases you can work around this for POJOs by providing
     * custom annotations, or a custom PropertyNamingStrategy, but we use Maps. To get around this, we create
     * two schemas: one with the names for serializing the actual data, and the other of label aliases for
     * the header row only.
     */
    private Pair<CsvSchema, CsvSchema> createSchema(EntityType entityType, List<String> fields) {
      List<EntityTypeColumn> usedColumns = entityType
        .getColumns()
        .stream()
        .filter(column -> fields.contains(column.getName()))
        .toList();

      CsvSchema.Builder builderName = CsvSchema.builder();
      CsvSchema.Builder builderLabelAlias = CsvSchema.builder();

      usedColumns.forEach(column -> builderName.addColumn(column.getName(), getColumnType(column)));
      usedColumns.forEach(column ->
        builderLabelAlias.addColumn(
          column.getLabelAlias().replaceAll("—", "-").replaceAll("–", "-"),
          getColumnType(column)
        )
      );

      // we want to use the original ET's ordering for the CSV (and this guarantees both will use the same order)
      String[] ordering = usedColumns.stream().map(EntityTypeColumn::getName).toArray(String[]::new);

      return Pair.of(builderName.build().sortedBy(ordering), builderLabelAlias.build().sortedBy(ordering));
    }

    private CsvSchema.ColumnType getColumnType(EntityTypeColumn column) {
      return COLUMN_TYPE_MAPPER.getOrDefault(column.getDataType().getDataType(), CsvSchema.ColumnType.STRING);
    }
  }
}
