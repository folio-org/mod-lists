package org.folio.list.repository;

import org.folio.list.domain.ListContent;
import org.folio.list.domain.ListContentId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ListContentsRepository extends JpaRepository<ListContent, ListContentId> {
  @Modifying
  @Query("DELETE FROM ListContent lc WHERE lc.listId = :listId AND lc.refreshId = :refreshId")
  void deleteContents(UUID listId, UUID refreshId);

  @Modifying
  @Query("DELETE FROM ListContent lc WHERE lc.listId = :listId")
  void deleteContents(UUID listId);

  @Query("""
    SELECT lc FROM ListContent lc
    WHERE lc.listId = :listId
    AND lc.refreshId = :refreshId
    ORDER BY lc.sortSequence ASC
    """)
  List<ListContent> getContents(UUID listId, UUID refreshId, Pageable page);

  @Query("""
    SELECT lc FROM ListContent lc
    WHERE lc.listId = :listId
    AND lc.refreshId = :refreshId
    AND lc.sortSequence > :afterSequence
    ORDER BY lc.sortSequence ASC
    """)
  List<ListContent> getContents(UUID listId, UUID refreshId, int afterSequence, Pageable page);
}
