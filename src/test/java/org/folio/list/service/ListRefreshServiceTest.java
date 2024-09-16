package org.folio.list.service;

import org.folio.list.domain.ListEntity;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListRepository;
import org.folio.list.rest.QueryClient;
import org.folio.list.services.EntityManagerFlushService;
import org.folio.list.services.refresh.ListRefreshService;
import org.folio.list.services.refresh.RefreshFailedCallback;
import org.folio.list.services.refresh.RefreshSuccessCallback;
import org.folio.list.services.refresh.TimedStage;
import org.folio.list.util.TaskTimer;
import org.folio.list.utils.TestDataFixture;
import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.querytool.domain.dto.QueryIdentifier;
import org.folio.querytool.domain.dto.SubmitQuery;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ListRefreshServiceTest {

  @Mock
  private QueryClient queryClient;
  @Mock
  private FolioExecutionContext executionContext;
  @Mock
  private ListRepository listRepository;
  @Mock
  private ListContentsRepository listContentsRepository;
  @Mock
  private RefreshSuccessCallback refreshSuccessCallback;
  @Mock
  private RefreshFailedCallback refreshFailedCallback;
  @Mock
  @Qualifier("listBatchCallbackSupplier")
  private Supplier<BiConsumer<ListEntity, List<UUID>>> listBatchCallbackSupplier;
  @InjectMocks
  private ListRefreshService listRefreshService;
  @Mock
  private EntityManagerFlushService entityManagerFlushService;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(listRefreshService, "getQueryTimeoutMinutes", 10);
  }

  @Test
  void shouldStartRefresh() {
    ListEntity list = TestDataFixture.getListEntityWithSuccessRefresh();
    int totalRecords = 0;
    ArgumentCaptor<SubmitQuery> submitQueryArgumentCaptor = ArgumentCaptor.forClass(SubmitQuery.class);
    QueryDetails queryDetails = new QueryDetails().status(QueryDetails.StatusEnum.SUCCESS).totalRecords(totalRecords);
    QueryIdentifier expectedIdentifier = new QueryIdentifier().queryId(UUID.randomUUID());
    when(queryClient.executeQuery(any())).thenReturn(expectedIdentifier);
    when(queryClient.getQuery(expectedIdentifier.getQueryId())).thenReturn(queryDetails);
    var timer = new TaskTimer();
    timer.start(TimedStage.TOTAL);
    listRefreshService.doAsyncRefresh(list, null, timer);
    verify(queryClient, times(1)).executeQuery(submitQueryArgumentCaptor.capture());
    verify(refreshSuccessCallback, times(1)).accept(list, totalRecords, timer, false);
  }

  @Test
  void shouldStartAsyncSort() {
    UUID queryId = UUID.randomUUID();
    ListEntity list = TestDataFixture.getListEntityWithSuccessRefresh();
    int totalRecords = 0;
    QueryDetails queryDetails = new QueryDetails().status(QueryDetails.StatusEnum.SUCCESS).totalRecords(totalRecords);
    when(queryClient.getQuery(queryId)).thenReturn(queryDetails);
    var timer =  new TaskTimer();
    timer.start(TimedStage.TOTAL);
    listRefreshService.doAsyncSorting(list, queryId, null, timer);
    verify(refreshSuccessCallback, times(1)).accept(list, totalRecords, timer, false);
  }

  @Test
  void shouldHandleQueryCancelledDuringRefresh() {
    ListEntity list = TestDataFixture.getListEntityWithSuccessRefresh();
    int totalRecords = 0;
    ArgumentCaptor<SubmitQuery> submitQueryArgumentCaptor = ArgumentCaptor.forClass(SubmitQuery.class);
    QueryDetails queryDetails = new QueryDetails().status(QueryDetails.StatusEnum.CANCELLED).totalRecords(totalRecords);
    QueryIdentifier expectedIdentifier = new QueryIdentifier().queryId(UUID.randomUUID());
    when(queryClient.executeQuery(any())).thenReturn(expectedIdentifier);
    when(queryClient.getQuery(expectedIdentifier.getQueryId())).thenReturn(queryDetails);
    var timer =  new TaskTimer();
    timer.start(TimedStage.TOTAL);
    listRefreshService.doAsyncRefresh(list, null, timer);
    verify(queryClient, times(1)).executeQuery(submitQueryArgumentCaptor.capture());
    verify(refreshFailedCallback, times(1)).accept(eq(list), eq(timer), any());
  }

  @Test
  void shouldHandleQueryFailedDuringRefresh() {
    ListEntity list = TestDataFixture.getListEntityWithSuccessRefresh();
    int totalRecords = 0;
    ArgumentCaptor<SubmitQuery> submitQueryArgumentCaptor = ArgumentCaptor.forClass(SubmitQuery.class);
    QueryDetails queryDetails = new QueryDetails().status(QueryDetails.StatusEnum.FAILED).totalRecords(totalRecords);
    QueryIdentifier expectedIdentifier = new QueryIdentifier().queryId(UUID.randomUUID());
    when(queryClient.executeQuery(any())).thenReturn(expectedIdentifier);
    when(queryClient.getQuery(expectedIdentifier.getQueryId())).thenReturn(queryDetails);
    var timer =  new TaskTimer();
    timer.start(TimedStage.TOTAL);
    listRefreshService.doAsyncRefresh(list, null, timer);
    verify(queryClient, times(1)).executeQuery(submitQueryArgumentCaptor.capture());
    verify(refreshFailedCallback, times(1)).accept(eq(list), eq(timer), any());
  }
}


