package org.folio.list.services.refresh;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.list.domain.ListEntity;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Log4j2
public class RefreshFailedCallback implements BiConsumer<ListEntity, Throwable> {
  private final ListRepository listRepository;
  private final ListContentsRepository listContentsRepository;

  @Transactional
  public void accept(ListEntity entity, Throwable failureReason) {
    saveFailedRefresh(entity, failureReason);
  }

  /**
   * Compare this list's inProgressRefreshId with the up-to-date inProgressRefreshId from the database.
   * Save list with refresh details if they are the same.
   */
  private void saveFailedRefresh(ListEntity entity, Throwable failureReason) {
    UUID currentRefreshId = entity.getInProgressRefreshId()
      .orElseThrow(() -> new IllegalStateException("List " + entity.getId() + " is not refreshing"));
    log.error("Refresh failed for list {}, refreshId {}. Reason for failure: {}",
      entity.getId(),
      currentRefreshId,
      failureReason);
    // inProgressRefresh should only be saved as failedRefresh if it is this list's most recent inProgressRefresh
    // Otherwise, it would overwrite the more recent refresh. However, contents of refresh should be deleted
    // no matter what
    if (isActiveRefresh(entity.getId(), currentRefreshId)) {
      entity.refreshFailed(failureReason);
      listRepository.save(entity);
    }
    listContentsRepository.deleteContents(entity.getId(), currentRefreshId);
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
