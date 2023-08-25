package org.folio.list.exception;

import org.folio.list.domain.ListEntity;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

public class ListNotRefreshingException extends SimpleListException {
  public ListNotRefreshingException(ListEntity list, ListActions failedAction) {
    super(list, failedAction, "Illegal State - list " + list.getId() + " is not refreshing", "list.not.refreshing", HttpStatus.BAD_REQUEST);
  }
}
