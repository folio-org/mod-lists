package org.folio.list.services.export;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
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
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.folio.list.exception.ExportNotFoundException.exportNotFound;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

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

  @SneakyThrows
  public ExportLocalStorage createCSV(ExportDetails exportDetails) {
    var localStorage = new ExportLocalStorage(exportDetails.getExportId());
    ListEntity list = exportDetails.getList();
    var idsProvider = new ListIdsProvider(contentsRepository, list);
    EntityType entityType = entityTypeClient.getEntityType(list.getEntityTypeId());

    try (var localStorageOutputStream = localStorage.outputStream()) {
      var csvWriter = new ListCsvWriter(entityType, localStorageOutputStream);
      int batchSize = exportProperties.getBatchSize();
      int batchNumber = 0;
      for (List<List<String>> ids = idsProvider.nextBatch(batchSize); !isEmpty(ids); ids = idsProvider.nextBatch(batchSize)) {
        if (batchNumber % 10 == 0) {
          checkIfExportCancelled(list.getId(), exportDetails.getExportId());
        }
        log.info("Export in progress for exportId {}. Fetched a batch of {} IDs.", exportDetails.getExportId(), ids.size());
        ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(list.getEntityTypeId())
          .fields(list.getFields())
          .ids(ids);
        var sortedContents = queryClient.getContents(contentsRequest);
        csvWriter.writeCsv(sortedContents);
        batchNumber++;
      }
    }
    return localStorage;
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
    private final OutputStream destination;
    private final ObjectWriter objectWriter;
    private boolean firstBatch;

    private static final Map<String, CsvSchema.ColumnType> COLUMN_TYPE_MAPPER = Map.of(
      "booleanType", CsvSchema.ColumnType.BOOLEAN,
      "numberType", CsvSchema.ColumnType.NUMBER,
      "arrayType", CsvSchema.ColumnType.ARRAY
    );

    private ListCsvWriter(EntityType entityType, OutputStream destination) {
      this.destination = destination;
      this.csvSchema = createSchema(entityType);
      this.objectWriter = new CsvMapper().writerFor(List.class);
      this.firstBatch = true;
    }

    @SneakyThrows
    public void writeCsv(List<Map<String, Object>> listContents) {
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
