package org.folio.list.services.refresh;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.list.domain.AsyncProcessStatus;
import org.folio.list.domain.ListContent;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.ListRefreshDetails;
import org.folio.list.domain.dto.ListConfiguration;
import org.folio.list.exception.ListNotRefreshingException;
import org.folio.list.exception.MaxListSizeExceededException;
import org.folio.list.exception.RefreshCancelledException;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListRefreshRepository;
import org.folio.list.services.ListActions;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.folio.list.domain.ListContent.SORT_SEQUENCE_START_NUMBER;

@RequiredArgsConstructor
@Log4j2
public class DataBatchCallback implements BiConsumer<ListEntity, List<UUID>> {
  private final ListRefreshRepository listRefreshRepository;
  private final ListContentsRepository listContentsRepository;
  private final ListConfiguration listConfiguration;
  private int batchNumber = 0;
  private int sortSequence = SORT_SEQUENCE_START_NUMBER;

  public void accept(ListEntity entity, List<UUID> contentIds) {
    UUID refreshId = entity.getInProgressRefreshId().orElseThrow(() -> new ListNotRefreshingException(entity, ListActions.REFRESH));
    log.info("Received data batch for list {}, refreshId {}: {}", entity.getId(), refreshId, contentIds);
    if (batchNumber % 10 == 0) {
      checkIfRefreshCancelled(entity, refreshId);
    }
    checkIfMaxListSizeExceeded(entity, sortSequence + contentIds.size());
    List<ListContent> batch = contentIds.stream()
      .map(id -> new ListContent(entity.getId(), refreshId, id, sortSequence++))
      .collect(Collectors.toList());
    listContentsRepository.saveAll(batch);
    log.info("Saved list contents; list ID: {}; refreshId: {}; records in this batch: {}; total records so far: {}",
      entity.getId(), refreshId, contentIds.size(), sortSequence);
    batchNumber++;
  }

  private void checkIfMaxListSizeExceeded(ListEntity entity, int currentSize) {
    if (currentSize > listConfiguration.getMaxListSize()) {
      log.info("List {} has exceeded maximum list size of {}. Marking refresh as failed.",
        entity.getId(), listConfiguration.getMaxListSize());
      throw new MaxListSizeExceededException(entity, listConfiguration.getMaxListSize());
    }
  }

  /**
   * Retrieve up-to-date inProgressRefresh from database to check if cancelled. If so, stop performing refresh
   */
  private void checkIfRefreshCancelled(ListEntity entity, UUID refreshId) {
    Optional<ListRefreshDetails> inProgressRefresh = listRefreshRepository.findById(refreshId);
    inProgressRefresh
      .filter(refresh -> refresh.getStatus() == AsyncProcessStatus.CANCELLED)
      .ifPresent(refresh -> {
        log.info("Refresh cancelled for list {}, refreshId {}", entity.getId(), refreshId);
        throw new RefreshCancelledException(entity);
      });
  }
}

