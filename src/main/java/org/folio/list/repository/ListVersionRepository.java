package org.folio.list.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.list.domain.ListVersion;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListVersionRepository
  extends CrudRepository<ListVersion, UUID> {
  List<ListVersion> findByListId(UUID listId);
  Optional<ListVersion> findByListIdAndVersion(UUID listId, int version);
}
