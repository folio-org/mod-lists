package org.folio.list.exception;

import org.folio.list.domain.ListEntity;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

public class ListInactiveException extends SimpleListException {
  public ListInactiveException(ListEntity list, ListActions failedAction) {
    super(list, failedAction, "List ( with id " + list.getId() + " ) is Inactive", "list.is.inactive", HttpStatus.BAD_REQUEST);
  }
}
