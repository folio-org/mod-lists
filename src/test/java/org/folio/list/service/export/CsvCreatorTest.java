package org.folio.list.service.export;

import org.folio.list.configuration.ListExportProperties;
import org.folio.list.domain.AsyncProcessStatus;
import org.folio.list.domain.ExportDetails;
import org.folio.list.domain.ListContent;
import org.folio.list.domain.ListEntity;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListExportRepository;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.rest.SystemUserQueryClient;
import org.folio.list.services.ListActions;
import org.folio.list.services.export.CsvCreator;
import org.folio.list.services.export.ExportLocalStorage;
import org.folio.list.utils.TestDataFixture;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.querytool.domain.dto.StringType;
import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvCreatorTest {
  @Mock
  private ListContentsRepository contentsRepository;
  @Mock
  private EntityTypeClient entityTypeClient;
  @Mock
  private SystemUserQueryClient queryClient;
  @Mock
  private ListExportRepository listExportRepository;
  @Mock(mockMaker = MockMakers.INLINE)
  private ListExportProperties exportProperties;
  @Mock
  private FolioS3Client folioS3Client;
  @InjectMocks
  private CsvCreator csvCreator;

  @Test
  void shouldCreateCsvFromList() throws IOException {
    int batchSize = 100000;
    UUID userId = UUID.randomUUID();
    String destinationFileName = "destinationFileName";
    String uploadId = "uploadId";
    var partETags = new ArrayList<String>();
    String partETag = "partETag";
    int firstPartNumber = 1;
    int secondPartNumber = 2;
    int numberOfBatch = 11;

    ListEntity entity = TestDataFixture.getPrivateListEntity();
    EntityType entityType = createEntityType(List.of(createColumn("id"), createColumn("item_status")));
    ExportDetails exportDetails = createExportDetails(entity, UUID.randomUUID());
    List<List<String>> contentIds = new ArrayList<>();
    // generate content ids for ten batches
    IntStream.rangeClosed(1, batchSize * numberOfBatch).forEach(i -> contentIds.add(List.of(UUID.randomUUID().toString())));
    List<Map<String, Object>> contentsWithData = new ArrayList<>();
    IntStream.rangeClosed(1, batchSize).forEach(i ->
      {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("id", "value1");
        linkedHashMap.put("item_status", "value2");
        contentsWithData.add(linkedHashMap);
      }
    );

    IntStream.rangeClosed(0, numberOfBatch - 1).forEach(i -> when(queryClient.getContentsPrivileged(
      new ContentsRequest().entityTypeId(entity.getEntityTypeId())
        .fields(entity.getFields())
        .localize(true)
        .userId(userId)
        .ids(contentIds.stream().skip((long) i * batchSize).limit(batchSize).toList()))).thenReturn(contentsWithData));

    AtomicInteger indexBatch = new AtomicInteger(0);
    IntStream.rangeClosed(0, numberOfBatch - 1).forEach(i ->
      when(contentsRepository.getContents(entity.getId(), entity.getSuccessRefresh().getId(), (i * batchSize) - 1, PageRequest.ofSize(batchSize)))
        .thenReturn(contentIds.stream().skip((long) i * batchSize).limit(batchSize)
          .map(id -> new ListContent(entity.getId(), entity.getSuccessRefresh().getId(), id, indexBatch.getAndIncrement()))
          .toList()));

    when(exportProperties.getBatchSize()).thenReturn(batchSize);
    when(entityTypeClient.getEntityType(entity.getEntityTypeId(), ListActions.EXPORT)).thenReturn(entityType);


    when(listExportRepository.findById(exportDetails.getExportId())).thenReturn(Optional.of(exportDetails));
    when(folioS3Client.uploadMultipartPart(eq(destinationFileName), eq(uploadId), eq(firstPartNumber), any())).thenReturn(partETag);
    when(folioS3Client.uploadMultipartPart(eq(destinationFileName), eq(uploadId), eq(secondPartNumber), any())).thenReturn(partETag);


    try (ExportLocalStorage csvStorage = csvCreator.createAndUploadCSV(exportDetails, destinationFileName, uploadId, partETags, userId)) {
      String actualCsv = new String(csvStorage.inputStream().readAllBytes());
      assertEquals(toCSV(contentsWithData), actualCsv);
      assertEquals(2, partETags.size());
    }
  }

  private static String toCSV(List<Map<String, Object>> list) {
    final StringBuilder sb = new StringBuilder();
    for (Map<String, Object> map : list) {
      int i = 0;
      for (Object value : map.values()) {
        sb.append(value);
        sb.append(i == map.keySet().size() - 1 ? "\n" : ",");
        i++;
      }
    }
    return sb.toString();
  }

  private EntityType createEntityType(List<EntityTypeColumn> entityTypeColumnList) {
    return new EntityType()
      .id(UUID.randomUUID().toString())
      .name("test_table")
      .labelAlias("derived_table_alias_01")
      .root(false)
      .columns(entityTypeColumnList);
  }

  private static EntityTypeColumn createColumn(String columnName) {
    return new EntityTypeColumn()
      .name(columnName)
      .dataType(new StringType().dataType("stringType"))
      .labelAlias("label_01")
      .visibleByDefault(false);
  }

  private static ExportDetails createExportDetails(ListEntity entity, UUID exportId) {
    ExportDetails exportDetails = new ExportDetails();
    exportDetails.setExportId(exportId);
    exportDetails.setList(entity);
    exportDetails.setStatus(AsyncProcessStatus.IN_PROGRESS);
    exportDetails.setFields(entity.getFields());
    return exportDetails;
  }
}
