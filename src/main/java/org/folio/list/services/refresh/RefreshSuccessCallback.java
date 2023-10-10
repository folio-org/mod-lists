package org.folio.list.services.refresh;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.list.domain.ListEntity;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListRepository;
import org.folio.list.util.TaskTimer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Log4j2
public class RefreshSuccessCallback implements SuccessCallback {
  private final ListRepository listRepository;
  private final ListContentsRepository listContentsRepository;

  @Transactional
  public void accept(ListEntity entity, int recordsCount, TaskTimer timer) {
    saveSuccessRefresh(entity, recordsCount, timer);
  }

  /**
   * Compare this list's inProgressRefreshId with the up-to-date inProgressRefreshId from the database.
   * Save list with refresh details if they are the same.
   */
  private void saveSuccessRefresh(ListEntity entity, Integer recordsCount, TaskTimer timer) {
    UUID currentRefreshId = entity.getInProgressRefreshId()
      .orElseThrow(() -> new IllegalStateException("List " + entity.getId() + " is not refreshing"));
    log.info("Refresh completed for list {}, refreshId {}. Total count: {}",
      entity.getId(),
      currentRefreshId,
      recordsCount);
    // Save list with refresh if this refresh is the most recent for the list. If not, delete the contents
    // of this refresh
    if (isActiveRefresh(entity.getId(), currentRefreshId)) {
        if (entity.getSuccessRefresh() != null) {
          listContentsRepository.deleteContents(entity.getId(), entity.getSuccessRefresh().getId());
        }
        entity.refreshCompleted(recordsCount);
        timer.time(TimedStage.WRITE_END, () -> listRepository.save(entity));
    } else {
      listContentsRepository.deleteContents(entity.getId(), currentRefreshId);
    }
  }

  /**
   * Check if the inProgressRefreshId of the list in memory matches the most up-to-date
   * inProgressRefreshId for this list in database.
   */
  private boolean isActiveRefresh(UUID listId, UUID refreshId) {
    return listRepository.findById(listId)
      .flatMap(ListEntity::getInProgressRefreshId)
      .filter(Predicate.isEqual(refreshId))
      .isPresent();
  }

}
