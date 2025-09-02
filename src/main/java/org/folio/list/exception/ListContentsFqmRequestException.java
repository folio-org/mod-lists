package org.folio.list.exception;

import org.folio.list.domain.ListEntity;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

public class ListContentsFqmRequestException extends SimpleListException {
  public ListContentsFqmRequestException(ListEntity list) {
    this(list, "This may be due to an upstream data schema change, in which case, refreshing the list may be sufficient to fix the issue.");
  }
  public ListContentsFqmRequestException(ListEntity list, String message) {
    super(list, ListActions.READ, "Failed to retrieve list contents for list " + list.getId() + ". " + message, "list.contents.request.failed", HttpStatus.CONFLICT);
  }
}
