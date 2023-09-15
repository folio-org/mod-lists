package org.folio.list.repository;

import org.folio.list.domain.ListEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ListRepository extends CrudRepository<ListEntity, UUID>, PagingAndSortingRepository<ListEntity, UUID> {

  @Query(
    value = """
      SELECT l
      FROM ListEntity l LEFT JOIN FETCH l.successRefresh s
      WHERE (COALESCE(:ids) IS NULL OR l.id IN (:ids))
      AND (COALESCE(:entityTypeIds) IS NULL OR l.entityTypeId IN (:entityTypeIds))
      AND (l.isPrivate = false OR l.updatedBy = :currentUserId OR ( l.updatedBy IS NULL AND l.createdBy = :currentUserId))
      AND (:isPrivate IS NULL OR l.isPrivate = :isPrivate)
      AND (:active IS NULL OR l.isActive = :active)
      AND (TO_TIMESTAMP(CAST(:updatedAsOf AS text), 'YYYY-MM-DD HH24:MI:SS.MS') IS NULL OR
      (l.createdDate>= TO_TIMESTAMP(CAST(:updatedAsOf AS text), 'YYYY-MM-DD HH24:MI:SS.MS') OR
      l.updatedDate>= TO_TIMESTAMP(CAST(:updatedAsOf AS text), 'YYYY-MM-DD HH24:MI:SS.MS')))
       ORDER BY l.name ASC, s.refreshEndDate DESC NULLS LAST
      """,
    countQuery = """
      SELECT count(*)
      FROM ListEntity l
      WHERE (COALESCE(:ids) IS NULL OR l.id IN (:ids))
      AND (COALESCE(:entityTypeIds) IS NULL OR l.entityTypeId IN (:entityTypeIds))
      AND (l.isPrivate = false OR l.updatedBy = :currentUserId OR ( l.updatedBy IS NULL AND l.createdBy = :currentUserId))
      AND (:isPrivate IS NULL OR l.isPrivate = :isPrivate)
      AND (:active IS NULL OR l.isActive = :active)
      AND (TO_TIMESTAMP(CAST(:updatedAsOf AS text), 'YYYY-MM-DD HH24:MI:SS.MS') IS NULL OR
      (l.createdDate>= TO_TIMESTAMP(CAST(:updatedAsOf AS text), 'YYYY-MM-DD HH24:MI:SS.MS') OR
      l.updatedDate>= TO_TIMESTAMP(CAST(:updatedAsOf AS text), 'YYYY-MM-DD HH24:MI:SS.MS')))
       """
 )
  Page<ListEntity> searchList(Pageable pageable, List<UUID> ids, List<UUID> entityTypeIds, UUID currentUserId, Boolean active, Boolean isPrivate, OffsetDateTime updatedAsOf);

}

