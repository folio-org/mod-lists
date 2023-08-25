package org.folio.list.services.refresh;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.fqm.lib.model.FqlQueryWithContext;
import org.folio.fqm.lib.service.QueryProcessorService;
import org.folio.fqm.lib.service.QueryResultsSorterService;
import org.folio.list.domain.ListEntity;
import org.folio.list.services.AppShutdownService.ShutdownTask;
import org.folio.spring.FolioExecutionContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Supplier;

@Service
@Log4j2
@RequiredArgsConstructor
public class ListRefreshService {

  private static final int DEFAULT_BATCH_SIZE = 1000;

  private final QueryProcessorService queryProcessorService;
  private final FolioExecutionContext executionContext;
  private final QueryResultsSorterService queryResultsSorterService;
  private final RefreshSuccessCallback refreshSuccessCallback;
  private final RefreshFailedCallback refreshFailedCallback;
  private final Supplier<DataBatchCallback> dataBatchCallbackSupplier;

  @Async
  // Long-running method. Running this method within a transaction boundary will hog db connection for
  // long time. Hence, do not run this method in a transaction. Start transactions programmatically in
  // call-back methods
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void doAsyncRefresh(ListEntity list, ShutdownTask shutdownTask) {
    try (var autoCloseMe = shutdownTask) {
      log.info("Performing async refresh for list {}, refreshId {}", list.getId(),
        list.getInProgressRefreshId().map(UUID::toString).orElse("NONE"));
      DataBatchCallback dataBatchCallback = dataBatchCallbackSupplier.get();
      var fqlQueryWithContext = new FqlQueryWithContext(executionContext.getTenantId(),
        list.getEntityTypeId(),
        list.getFqlQuery(),
        true);
      queryProcessorService.getIdsInBatch(
        fqlQueryWithContext,
        DEFAULT_BATCH_SIZE,
        batch -> dataBatchCallback.accept(list, batch.ids()),
        recordsCount -> refreshSuccessCallback.accept(list, recordsCount),
        throwable -> refreshFailedCallback.accept(list, throwable)
      );
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
      DataBatchCallback dataBatchCallback = dataBatchCallbackSupplier.get();
      queryResultsSorterService.streamSortedIds(
        executionContext.getTenantId(),
        queryId,
        DEFAULT_BATCH_SIZE,
        batch -> dataBatchCallback.accept(list, batch.ids()),
        recordsCount -> refreshSuccessCallback.accept(list, recordsCount),
        throwable -> refreshFailedCallback.accept(list, throwable)
      );
    } catch (Exception exception) {
      log.error("Unexpected error when performing async refresh for list with id " + list.getId()
        + ", refreshId " + (list.getInProgressRefreshId().map(UUID::toString).orElse("NONE")), exception);
      refreshFailedCallback.accept(list, exception);
    }
  }
}
