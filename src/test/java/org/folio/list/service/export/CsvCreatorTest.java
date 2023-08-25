package org.folio.list.service.export;

import org.folio.fqm.lib.service.FqmMetaDataService;
import org.folio.fqm.lib.service.ResultSetService;
import org.folio.list.configuration.ListExportProperties;
import org.folio.list.domain.AsyncProcessStatus;
import org.folio.list.domain.ExportDetails;
import org.folio.list.domain.ListContent;
import org.folio.list.domain.ListEntity;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListExportRepository;
import org.folio.list.services.export.CsvCreator;
import org.folio.list.services.export.ExportLocalStorage;
import org.folio.list.utils.TestDataFixture;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.querytool.domain.dto.StringType;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvCreatorTest {
  @Mock
  private ListContentsRepository contentsRepository;
  @Mock
  private FqmMetaDataService metaDataService;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private ResultSetService resultSetService;
  @Mock
  private ListExportRepository listExportRepository;
  @Mock(mockMaker = MockMakers.INLINE)
  private ListExportProperties exportProperties;
  @InjectMocks
  private CsvCreator csvCreator;

  @Test
  void shouldCreateCsvFromList() throws IOException {
    String tenantId = "tenant_01";
    int batchSize = 100;

    ListEntity entity = TestDataFixture.getPrivateListEntity();
    EntityType entityType = createEntityType(List.of(createColumn("col1"), createColumn("col2")));
    ExportDetails exportDetails = createExportDetails(entity, UUID.randomUUID());

    List<UUID> contentIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    List<ListContent> contents = contentIds.stream()
      .map(id -> new ListContent(entity.getId(), entity.getSuccessRefresh().getId(), id, 1))
      .toList();
    List<Map<String, Object>> contentsWithData = List.of(
      Map.of("col1", "col1-value1", "col2", "col2-value1"),
      Map.of("col1", "col1-value2", "col2", "col2-value2")
    );

    String expectedCsv = """
      col1,col2
      col1-value1,col2-value1
      col1-value2,col2-value2
      """;

    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(exportProperties.getBatchSize()).thenReturn(batchSize);
    when(metaDataService.getEntityTypeDefinition(tenantId, entity.getEntityTypeId()))
      .thenReturn(Optional.of(entityType));
    when(contentsRepository.getContents(entity.getId(), entity.getSuccessRefresh().getId(), -1, PageRequest.ofSize(batchSize)))
      .thenReturn(contents);
    when(resultSetService.getResultSet(tenantId, entity.getEntityTypeId(), entity.getFields(), contentIds))
      .thenReturn(contentsWithData);
    when(listExportRepository.findById(exportDetails.getExportId())).thenReturn(Optional.of(exportDetails));

    try (ExportLocalStorage csvStorage = csvCreator.createCSV(exportDetails)) {
      String actualCsv = new String(csvStorage.inputStream().readAllBytes());
      assertEquals(expectedCsv, actualCsv);
    }
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
    return exportDetails;
  }
}
