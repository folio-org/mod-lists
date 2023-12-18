package org.folio.list.repository;

import org.folio.list.domain.ListVersion;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ListVersionRepository extends CrudRepository<ListVersion, UUID> {
  List<ListVersion> findByListId(UUID listId);
}
