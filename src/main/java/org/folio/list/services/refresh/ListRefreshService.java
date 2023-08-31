package org.folio.list.services.refresh;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.awaitility.Awaitility;
import org.folio.list.domain.ListEntity;
import org.folio.list.exception.RefreshCancelledException;
import org.folio.list.rest.QueryClient;
import org.folio.list.services.AppShutdownService.ShutdownTask;
import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.querytool.domain.dto.QueryIdentifier;
import org.folio.querytool.domain.dto.SubmitQuery;
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
  private static final int GET_QUERY_TIMEOUT_MINUTES = 30;
  private static final int DEFAULT_BATCH_SIZE = 1000;

  private final RefreshSuccessCallback refreshSuccessCallback;
  private final RefreshFailedCallback refreshFailedCallback;
  private final Supplier<DataBatchCallback> dataBatchCallbackSupplier;
  private final QueryClient queryClient;

  @Async
  // Long-running method. Running this method within a transaction boundary will hog db connection for
  // long time. Hence, do not run this method in a transaction. Start transactions programmatically in
  // call-back methods
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void doAsyncRefresh(ListEntity list, ShutdownTask shutdownTask) {
    try (var autoCloseMe = shutdownTask) {
      log.info("Performing async refresh for list {}, refreshId {}", list.getId(),
        list.getInProgressRefreshId().map(UUID::toString).orElse("NONE"));
      SubmitQuery submitQuery = new SubmitQuery()
        .entityTypeId(list.getEntityTypeId())
        .fqlQuery(list.getFqlQuery())
        .fields(list.getFields());
      QueryIdentifier queryIdentifier = queryClient.executeQuery(submitQuery);
      waitForQueryCompletion(list, queryIdentifier.getQueryId());
    } catch (Exception exception) {
      log.error("Unexpected error when performing async refresh for list with id " + list.getId()
        + ", refreshId " + (list.getInProgressRefreshId().map(UUID::toString).orElse("NONE")), exception);
      refreshFailedCallback.accept(list, exception);
    }
  }

  @Async
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void doAsyncSorting(ListEntity list, UUID queryId, ShutdownTask shutdownTask) {
    try (var autoCloseMe = shutdownTask) {
      log.info("Performing async sorting for list {}, refreshId {}", list.getId(),
        list.getInProgressRefreshId().map(UUID::toString).orElse("NONE"));
      waitForQueryCompletion(list, queryId);
    } catch (Exception exception) {
      log.error("Unexpected error when performing async refresh for list with id " + list.getId()
        + ", refreshId " + (list.getInProgressRefreshId().map(UUID::toString).orElse("NONE")), exception);
      refreshFailedCallback.accept(list, exception);
    }
  }

  private void waitForQueryCompletion(ListEntity list, UUID queryId) {
    log.info("Waiting for completion of query {} for list {}", queryId, list.getId());
    Awaitility.with()
      .pollInterval(GET_QUERY_TIME_DELAY_SECONDS, TimeUnit.SECONDS)
      .await()
      .atMost(GET_QUERY_TIMEOUT_MINUTES, TimeUnit.MINUTES)
      .until(() -> queryClient.getQuery(queryId).getStatus() != QueryDetails.StatusEnum.IN_PROGRESS);
    QueryDetails queryDetails = queryClient.getQuery(queryId);

    if (queryDetails.getStatus() == QueryDetails.StatusEnum.SUCCESS) {
      int resultCount = importQueryResults(list, queryId);
      refreshSuccessCallback.accept(list, resultCount);
    } else if (queryDetails.getStatus() == QueryDetails.StatusEnum.FAILED) {
      refreshFailedCallback.accept(list, new RuntimeException(queryDetails.getFailureReason()));
    } else if (queryDetails.getStatus() == QueryDetails.StatusEnum.CANCELLED) {
      refreshFailedCallback.accept(list, new RefreshCancelledException(list));
    }
  }

  private int importQueryResults(ListEntity list, UUID queryId) {
    log.info("Performing async sorting for list {}, refreshId {}", list.getId(),
      list.getInProgressRefreshId().map(UUID::toString).orElse("NONE"));
    DataBatchCallback dataBatchCallback = dataBatchCallbackSupplier.get();
    int offset = 0;
    List<UUID> ids = queryClient.getSortedIds(queryId, offset, DEFAULT_BATCH_SIZE);
    while (!CollectionUtils.isEmpty(ids)) {
      offset += ids.size();
      dataBatchCallback.accept(list, ids);
      ids = queryClient.getSortedIds(queryId, offset, DEFAULT_BATCH_SIZE);
    }
    return offset;
  }
}
