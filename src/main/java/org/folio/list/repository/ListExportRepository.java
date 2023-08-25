package org.folio.list.repository;

import org.folio.list.domain.ExportDetails;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListExportRepository extends CrudRepository<ExportDetails, UUID> {

  Optional<ExportDetails> findByListIdAndExportId(UUID listId, UUID exportId);

  @Query("""
    SELECT CASE
    WHEN(COUNT(*) > 0)
    THEN TRUE
    ELSE FALSE
    END
    FROM ExportDetails ed WHERE ed.list.id = :listId and ed.status = 'IN_PROGRESS'
    """)
  boolean isExporting(UUID listId);

  @Query("""
    SELECT CASE
    WHEN(COUNT(*) > 0)
    THEN TRUE
    ELSE FALSE
    END
    FROM ExportDetails ed WHERE ed.list.id = :listId and ed.status = 'IN_PROGRESS' and ed.createdBy = :userId
    """)
  boolean isUserAlreadyExporting(UUID listId, UUID userId);
}
