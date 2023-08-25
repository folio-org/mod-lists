package org.folio.list.service;

import org.folio.fqm.lib.model.FqlQueryWithContext;
import org.folio.fqm.lib.service.QueryResultsSorterService;
import org.folio.fqm.lib.service.QueryProcessorService;
import org.folio.list.domain.ListEntity;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListRepository;
import org.folio.list.services.refresh.ListRefreshService;
import org.folio.list.utils.TestDataFixture;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ListRefreshServiceTest {

  @Mock
  private QueryProcessorService queryProcessorService;
  @Mock
  private QueryResultsSorterService queryResultsService;
  @Mock
  private FolioExecutionContext executionContext;
  @Mock
  private ListRepository listRepository;
  @Mock
  private ListContentsRepository listContentsRepository;
  @Mock
  @Qualifier("listBatchCallbackSupplier")
  private Supplier<BiConsumer<ListEntity, List<UUID>>> listBatchCallbackSupplier;
  @InjectMocks
  private ListRefreshService listRefreshService;

  @Test
  void shouldStartRefresh() {
    String tenantId = "Tenant_01";
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh();
    when(executionContext.getTenantId()).thenReturn(tenantId);
    listRefreshService.doAsyncRefresh(entity, null);
    FqlQueryWithContext fqlQueryWithContext = new FqlQueryWithContext(tenantId, entity.getEntityTypeId(), entity.getFqlQuery(), true);

    verify(queryProcessorService, times(1)).getIdsInBatch(
      eq(fqlQueryWithContext),
      anyInt(),
      any(),
      any(),
      any());
  }

  @Test
  void shouldStartAsyncSort() {
    String tenantId = "Tenant_01";
    UUID queryId = UUID.randomUUID();
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh();
    when(executionContext.getTenantId()).thenReturn(tenantId);
    listRefreshService.doAsyncSorting(entity, queryId, null);

    verify(queryResultsService, times(1)).streamSortedIds(
      eq(tenantId),
      eq(queryId),
      eq(1000),
      any(),
      any(),
      any());
  }
}


