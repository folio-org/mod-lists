package org.folio.list.exception;

import org.folio.list.domain.ListEntity;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

public class ListIsCannedException extends SimpleListException {
  public ListIsCannedException(ListEntity list, ListActions failedAction) {
    super(list, failedAction, "List ( with id " + list.getId() + " ) is canned", "list.is.canned", HttpStatus.BAD_REQUEST);
  }
}

