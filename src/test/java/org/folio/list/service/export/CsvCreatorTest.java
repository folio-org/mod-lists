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
import org.folio.s3.exception.S3ClientException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.io.File;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.io.FileUtils;

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
  void shouldCreateCsvFromList() {
    int batchSize = 100000;
    UUID userId = UUID.fromString("8cbb467d-629f-5fbe-bcf0-515eee16cddc");
    String destinationFileName = "destinationFileName";
    String uploadId = "uploadId";
    List<String> partETags = new ArrayList<>();
    String partETag = "partETag";
    int numberOfBatch = 11;

    ListEntity entity = TestDataFixture.getPrivateListEntity();
    EntityType entityType = createEntityType(List.of(createColumn("id"), createColumn("item_status")));
    ExportDetails exportDetails = createExportDetails(entity, UUID.randomUUID());
    List<List<String>> contentIds = new ArrayList<>();
    // generate content ids for ten batches
    IntStream.rangeClosed(1, batchSize * numberOfBatch).forEach(i -> contentIds.add(List.of(new UUID(0, i).toString())));
    List<Map<String, Object>> contentsWithData = new ArrayList<>();
    IntStream.rangeClosed(1, batchSize).forEach(i -> {
      LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<>();
      linkedHashMap.put("id", "value1");
      linkedHashMap.put("item_status", "value2");
      contentsWithData.add(linkedHashMap);
    });

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

    StringWriter data = new StringWriter();

    when(listExportRepository.findById(exportDetails.getExportId())).thenReturn(Optional.of(exportDetails));
    when(folioS3Client.uploadMultipartPart(eq(destinationFileName), eq(uploadId), anyInt(), any())).thenAnswer(i -> {
      data.append(FileUtils.readFileToString(new File((String) i.getArgument(3)), "UTF-8"));
      return partETag;
    });

    try (ExportLocalStorage csvStorage = csvCreator.createAndUploadCSV(exportDetails, destinationFileName, uploadId, partETags, userId)) {
      String actual = data.toString();
      String expected = "[id-label],[item_status-label]\n" + toCSV(contentsWithData).repeat(numberOfBatch);
      assertEquals(actual, expected);
      assertEquals(2, partETags.size());
    }

    verify(folioS3Client, times(2)).uploadMultipartPart(eq(destinationFileName), eq(uploadId), anyInt(), any());
  }

  @Test
  void shouldRetryUploadPartIfExceptionIsThrown() {
    int batchSize = 100000;
    UUID userId = UUID.randomUUID();
    String destinationFileName = "destinationFileName";
    String uploadId = "uploadId";
    var partETags = new ArrayList<String>();
    int firstPartNumber = 1;

    ListEntity entity = TestDataFixture.getPrivateListEntity();
    EntityType entityType = createEntityType(List.of(createColumn("id"), createColumn("item_status")));
    ExportDetails exportDetails = createExportDetails(entity, UUID.randomUUID());

    when(exportProperties.getBatchSize()).thenReturn(batchSize);
    when(entityTypeClient.getEntityType(entity.getEntityTypeId(), ListActions.EXPORT)).thenReturn(entityType);
    when(folioS3Client.uploadMultipartPart(eq(destinationFileName), eq(uploadId), eq(firstPartNumber), any())).thenThrow(S3ClientException.class);

    assertThrows(S3ClientException.class, () -> csvCreator.createAndUploadCSV(exportDetails, destinationFileName, uploadId, partETags, userId));
    verify(folioS3Client, times(5)).uploadMultipartPart(eq(destinationFileName), eq(uploadId), eq(firstPartNumber), any());
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
      .labelAlias("[%s—label]".formatted(columnName))
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
