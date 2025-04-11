package org.folio.list.service;

import org.folio.list.domain.ListEntity;
import org.folio.list.domain.dto.ListConfiguration;
import org.folio.list.exception.MaxListSizeExceededException;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListRepository;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.rest.QueryClient;
import org.folio.list.services.EntityManagerFlushService;
import org.folio.list.services.refresh.ListRefreshService;
import org.folio.list.services.refresh.RefreshFailedCallback;
import org.folio.list.services.refresh.RefreshSuccessCallback;
import org.folio.list.services.refresh.DataBatchCallback;
import org.folio.list.services.refresh.TimedStage;
import org.folio.list.util.TaskTimer;
import org.folio.list.util.TestDataFixture;
import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.querytool.domain.dto.QueryIdentifier;
import org.folio.querytool.domain.dto.SubmitQuery;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.spring.FolioExecutionContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
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
  private Supplier<DataBatchCallback> dataBatchCallbackSupplier;
  @Mock
  private DataBatchCallback dataBatchCallback;
  @InjectMocks
  private ListRefreshService listRefreshService;
  @Mock
  private EntityManagerFlushService entityManagerFlushService;
  @Mock
  private ListConfiguration listConfiguration;
  @Mock
  private EntityTypeClient entityTypeClient;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(listRefreshService, "getQueryTimeoutMinutes", 10);
  }

  @Test
  void shouldStartRefresh() {
    ListEntity list = TestDataFixture.getListEntityWithSuccessRefresh();
    int totalRecords = 1;
    UUID queryId = UUID.randomUUID();
    QueryIdentifier expectedIdentifier = new QueryIdentifier().queryId(queryId);
    EntityType mockEntityType = getEntityType();
    when(entityTypeClient.getEntityType(any(UUID.class))).thenReturn(mockEntityType);
    when(queryClient.executeQuery(any())).thenReturn(expectedIdentifier);
    when(queryClient.getQuery(queryId, false, 0, 1000))
      .thenReturn(new QueryDetails().status(QueryDetails.StatusEnum.SUCCESS));
    List<Map<String, Object>> queryContent = List.of(Map.of("id", "123"));
    when(queryClient.getQuery(eq(queryId), eq(true), anyInt(), anyInt()))
      .thenReturn(new QueryDetails().status(QueryDetails.StatusEnum.SUCCESS).content(queryContent))
      .thenReturn(new QueryDetails().status(QueryDetails.StatusEnum.SUCCESS).content(Collections.emptyList()));
    when(dataBatchCallbackSupplier.get()).thenReturn(dataBatchCallback);
    var timer = new TaskTimer();
    timer.start(TimedStage.TOTAL);
    listRefreshService.doAsyncRefresh(list, null, timer);
    verify(refreshSuccessCallback, times(1)).accept(list, totalRecords, timer);
  }

  @Test
  void shouldStartAsyncSort() {
    UUID queryId = UUID.randomUUID();
    ListEntity list = TestDataFixture.getListEntityWithSuccessRefresh();
    int totalRecords = 1;
    EntityType mockEntityType = getEntityType();
    when(entityTypeClient.getEntityType(any(UUID.class))).thenReturn(mockEntityType);
    List<Map<String, Object>> queryContent = List.of(Map.of("id", "123"));
    when(dataBatchCallbackSupplier.get()).thenReturn(dataBatchCallback);
    when(queryClient.getQuery(queryId, false, 0, 1000))
      .thenReturn(new QueryDetails()
        .status(QueryDetails.StatusEnum.SUCCESS)
        .totalRecords(totalRecords)
      );
    when(queryClient.getQuery(eq(queryId), eq(true), anyInt(), anyInt()))
      .thenReturn(new QueryDetails().status(QueryDetails.StatusEnum.SUCCESS).content(queryContent))
      .thenReturn(new QueryDetails().status(QueryDetails.StatusEnum.SUCCESS).content(Collections.emptyList()));
    var timer = new TaskTimer();
    timer.start(TimedStage.TOTAL);
    listRefreshService.doAsyncSorting(list, queryId, null, timer);
    verify(refreshSuccessCallback, times(1)).accept(list, totalRecords, timer);
  }

  @Test
  void shouldHandleQueryCancelledDuringRefresh() {
    ListEntity list = TestDataFixture.getListEntityWithSuccessRefresh();
    int totalRecords = 0;
    ArgumentCaptor<SubmitQuery> submitQueryArgumentCaptor = ArgumentCaptor.forClass(SubmitQuery.class);
    EntityType mockEntityType = getEntityType();
    when(entityTypeClient.getEntityType(any(UUID.class))).thenReturn(mockEntityType);
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
    EntityType mockEntityType = getEntityType();
    when(entityTypeClient.getEntityType(any(UUID.class))).thenReturn(mockEntityType);
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

  @Test
  void shouldHandleMaxQuerySizeExceededDuringRefresh() {
    ListEntity list = TestDataFixture.getListEntityWithSuccessRefresh();
    int totalRecords = 0;
    ArgumentCaptor<SubmitQuery> submitQueryArgumentCaptor = ArgumentCaptor.forClass(SubmitQuery.class);
    EntityType mockEntityType = getEntityType();
    when(entityTypeClient.getEntityType(any(UUID.class))).thenReturn(mockEntityType);
    QueryDetails queryDetails = new QueryDetails().status(QueryDetails.StatusEnum.MAX_SIZE_EXCEEDED).totalRecords(totalRecords);
    QueryIdentifier expectedIdentifier = new QueryIdentifier().queryId(UUID.randomUUID());
    when(queryClient.executeQuery(any())).thenReturn(expectedIdentifier);
    when(queryClient.getQuery(expectedIdentifier.getQueryId(), false, 0, 1000)).thenReturn(queryDetails);
    when(listConfiguration.getMaxListSize()).thenReturn(10);
    var timer =  new TaskTimer();
    timer.start(TimedStage.TOTAL);
    listRefreshService.doAsyncRefresh(list, null, timer);
    verify(queryClient, times(1)).executeQuery(submitQueryArgumentCaptor.capture());
    verify(refreshFailedCallback, times(1)).accept(eq(list), eq(timer), any(MaxListSizeExceededException.class));
  }

  @NotNull
  private static EntityType getEntityType() {
    EntityType mockEntityType = new EntityType();
    EntityTypeColumn entityTypeColumn = new EntityTypeColumn();
    entityTypeColumn.setIsIdColumn(true);
    entityTypeColumn.setName("id");
    mockEntityType.setColumns(List.of(entityTypeColumn));
    return mockEntityType;
  }
}


