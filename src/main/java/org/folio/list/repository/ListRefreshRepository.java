package org.folio.list.repository;

import org.folio.list.domain.ListRefreshDetails;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ListRefreshRepository extends CrudRepository<ListRefreshDetails, UUID> {
}
