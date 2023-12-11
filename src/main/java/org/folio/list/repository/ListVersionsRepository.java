package org.folio.list.repository;

import org.folio.list.domain.ListVersions;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListVersionsRepository extends CrudRepository<ListVersions, UUID> {
  Optional<org.folio.list.domain.dto.ListVersionsDTO> findByListDetailsIdOrderByCreatedAtDesc(UUID listId);
}
