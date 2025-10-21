package org.folio.list.services.refresh;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.awaitility.Awaitility;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.dto.ListConfiguration;
import org.folio.list.exception.MaxListSizeExceededException;
import org.folio.list.exception.RefreshCancelledException;
import org.folio.list.rest.QueryClient;
import org.folio.list.services.AppShutdownService.ShutdownTask;
import org.folio.list.services.EntityManagerFlushService;
import org.folio.list.util.TaskTimer;
import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.querytool.domain.dto.QueryIdentifier;
import org.folio.querytool.domain.dto.SubmitQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@Log4j2
@RequiredArgsConstructor
public class ListRefreshService {

  private static final int GET_QUERY_TIME_DELAY_SECONDS = 10;
  @Value("${mod-lists.general.refresh-query-timeout-minutes:90}")
  private int getQueryTimeoutMinutes;
  @Value("${mod-lists.general.refresh-batch-size:10000}")
  private int refreshBatchSize;

  private final RefreshSuccessCallback refreshSuccessCallback;
  private final RefreshFailedCallback refreshFailedCallback;
  private final Supplier<DataBatchCallback> dataBatchCallbackSupplier;
  private final QueryClient queryClient;
  private final EntityManagerFlushService entityManagerFlushService;
  private final ListConfiguration listConfiguration;

  @Async
  // Long-running method. Running this method within a transaction boundary will hog db connection for
  // long time. Hence, do not run this method in a transaction. Start transactions programmatically in
  // call-back methods
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void doAsyncRefresh(ListEntity list, ShutdownTask shutdownTask, TaskTimer timer) {
    try (var autoCloseMe = shutdownTask) {
      log.info("Performing async refresh for list {}, refreshId {}", list.getId(),
        list.getInProgressRefreshId().map(UUID::toString).orElse("NONE"));
      SubmitQuery submitQuery = new SubmitQuery()
        .entityTypeId(list.getEntityTypeId())
        .fqlQuery(list.getFqlQuery())
        .fields(list.getFields());
      QueryIdentifier queryIdentifier = timer.time(TimedStage.REQUEST_QUERY, () -> queryClient.executeQuery(submitQuery));
      waitForQueryCompletion(list, queryIdentifier.getQueryId(), timer);
    } catch (Exception exception) {
      log.error("Unexpected error when performing async refresh for list with id " + list.getId()
        + ", refreshId " + (list.getInProgressRefreshId().map(UUID::toString).orElse("NONE")), exception);
      refreshFailedCallback.accept(list, timer, exception);
    }
    timer.stop(TimedStage.TOTAL);
  }

  @Async
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void doAsyncSorting(ListEntity list, UUID queryId, ShutdownTask shutdownTask, TaskTimer timer) {
    try (var autoCloseMe = shutdownTask) {
      log.info("Performing async sorting for list {}, refreshId {}", list.getId(),
        list.getInProgressRefreshId().map(UUID::toString).orElse("NONE"));
      waitForQueryCompletion(list, queryId, timer);
    } catch (Exception exception) {
      log.error("Unexpected error when performing async refresh for list with id " + list.getId()
        + ", refreshId " + (list.getInProgressRefreshId().map(UUID::toString).orElse("NONE")), exception);
      refreshFailedCallback.accept(list, timer, exception);
    }
    timer.stop(TimedStage.TOTAL);
  }

  private void waitForQueryCompletion(ListEntity list, UUID queryId, TaskTimer timer) {
    log.info("Waiting for completion of query {} for list {}", queryId, list.getId());
    timer.time(TimedStage.WAIT_FOR_QUERY_COMPLETION,
      () -> Awaitility.with()
        .pollInterval(GET_QUERY_TIME_DELAY_SECONDS, TimeUnit.SECONDS)
        .await()
        .atMost(getQueryTimeoutMinutes, TimeUnit.MINUTES)
        .until(() -> queryClient.getQuery(queryId).getStatus() != QueryDetails.StatusEnum.IN_PROGRESS));
    log.info("Query {} completed for list {}", queryId, list.getId());
    QueryDetails queryDetails = queryClient.getQuery(queryId);

    if (queryDetails.getStatus() == QueryDetails.StatusEnum.SUCCESS) {
      int resultCount = timer.time(TimedStage.IMPORT_RESULTS, () -> importQueryResults(list, queryId));
      refreshSuccessCallback.accept(list, resultCount, timer);
    } else if (queryDetails.getStatus() == QueryDetails.StatusEnum.FAILED) {
      refreshFailedCallback.accept(list, timer, new RuntimeException(queryDetails.getFailureReason()));
    } else if (queryDetails.getStatus() == QueryDetails.StatusEnum.MAX_SIZE_EXCEEDED) {
      // Technically this isn't perfect because the max list size and max query size could be different values,
      // but they should be equivalent in all real-world scenarios since they default to the same values and are
      // not explicitly overridden anywhere
      refreshFailedCallback.accept(list, timer, new MaxListSizeExceededException(list, listConfiguration.getMaxListSize()));
    }
    else if (queryDetails.getStatus() == QueryDetails.StatusEnum.CANCELLED) {
      refreshFailedCallback.accept(list, timer, new RefreshCancelledException(list));
    }
    entityManagerFlushService.flush();
  }

  private int importQueryResults(ListEntity list, UUID queryId) {
    log.info("Performing async sorting for list {}, refreshId {}", list.getId(),
      list.getInProgressRefreshId().map(UUID::toString).orElse("NONE"));
    DataBatchCallback dataBatchCallback = dataBatchCallbackSupplier.get();
    int offset = 0;
    List<List<String>> ids = queryClient.getSortedIds(queryId, offset, refreshBatchSize);
    while (!CollectionUtils.isEmpty(ids)) {
      offset += ids.size();
      dataBatchCallback.accept(list, ids);
      ids = queryClient.getSortedIds(queryId, offset, refreshBatchSize);
    }
    return offset;
  }
}
