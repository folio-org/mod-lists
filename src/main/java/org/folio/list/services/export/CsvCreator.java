package org.folio.list.services.export;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.folio.list.configuration.ListExportProperties;
import org.folio.list.domain.AsyncProcessStatus;
import org.folio.list.domain.ExportDetails;
import org.folio.list.domain.ListEntity;
import org.folio.list.exception.ExportCancelledException;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListExportRepository;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.rest.QueryClient;
import org.folio.list.services.ListActions;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.s3.client.FolioS3Client;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.list.exception.ExportNotFoundException.exportNotFound;

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
  private final QueryClient queryClient;
  private final EntityTypeClient entityTypeClient;
  private final FolioS3Client folioS3Client;

  //Minimal s3 part size is 5 MB
  private final Long MINIMAL_PART_SIZE = 5242880L;

  @SneakyThrows
  public ExportLocalStorage createAndUploadCSV(ExportDetails exportDetails, String destinationFileName, String uploadId, List<String> partETags) {
    var localStorage = new ExportLocalStorage(exportDetails.getExportId());
    ListEntity list = exportDetails.getList();
    var idsProvider = new ListIdsProvider(contentsRepository, list);
    EntityType entityType = entityTypeClient.getEntityType(list.getEntityTypeId());

    OutputStream localStorageOutputStream = localStorage.outputStream();
    var csvWriter = new ListCsvWriter(entityType);
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
      ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(list.getEntityTypeId())
        .fields(exportDetails.getFields())
        .ids(ids);
      var sortedContents = queryClient.getContents(contentsRequest);
      csvWriter.writeCsv(sortedContents, localStorageOutputStream);
      batchNumber++;
    }

    uploadCSVPart(destinationFileName, uploadId, partNumber, localStorage.getAbsolutePath(), partETags, exportDetails);

    return localStorage;


  }

  private void uploadCSVPart(String destinationFileName, String uploadId, int partNumber, String localStorage,
                             List<String> partETags, ExportDetails exportDetails) {
    String partETag = folioS3Client.uploadMultipartPart(destinationFileName, uploadId, partNumber, localStorage);
    partETags.add(partETag);
    log.info("Generated CSV multipart file for exportID {}. Uploading to S3, Part Number {}", exportDetails.getExportId(), partNumber);
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
    private final CsvSchema csvSchema;
    private final ObjectWriter objectWriter;
    private boolean firstBatch;

    private static final Map<String, CsvSchema.ColumnType> COLUMN_TYPE_MAPPER = Map.of(
      "booleanType", CsvSchema.ColumnType.BOOLEAN,
      "numberType", CsvSchema.ColumnType.NUMBER,
      "arrayType", CsvSchema.ColumnType.ARRAY
    );

    private ListCsvWriter(EntityType entityType) {
      this.csvSchema = createSchema(entityType);
      this.objectWriter = new CsvMapper().writerFor(List.class);
      this.firstBatch = true;
    }

    @SneakyThrows
    public void writeCsv(List<Map<String, Object>> listContents, OutputStream destination) {
      objectWriter
        .with(firstBatch ? csvSchema.withHeader() : csvSchema)
        .writeValues(destination)
        .write(listContents);
      firstBatch = false;
      destination.flush();
    }

    private CsvSchema createSchema(EntityType entityType) {
      CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
      entityType.getColumns().forEach(column -> csvSchemaBuilder.addColumn(column.getName(), getColumnType(column)));
      return csvSchemaBuilder.build();
    }

    private CsvSchema.ColumnType getColumnType(EntityTypeColumn column) {
      return COLUMN_TYPE_MAPPER.getOrDefault(column.getDataType().getDataType(), CsvSchema.ColumnType.STRING);
    }
  }
}
