package org.folio.list.exception;

import org.folio.list.domain.ListEntity;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

public class ListContentsFqmRequestException extends SimpleListException {
  public ListContentsFqmRequestException(ListEntity list) {
    super(list, ListActions.READ, "Failed to retrieve list contents for list " + list.getId() + ". This may be due to an upstream data schema change. Please refresh the list.", "list.contents.request.failed", HttpStatus.CONFLICT);
  }
}
