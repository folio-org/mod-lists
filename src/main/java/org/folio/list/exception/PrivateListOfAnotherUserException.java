package org.folio.list.exception;

import org.folio.list.domain.ListEntity;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

public class PrivateListOfAnotherUserException extends SimpleListException {
  public PrivateListOfAnotherUserException(ListEntity list, ListActions failedAction) {
    super(list, failedAction, "List ( with id " + list.getId() + " ) is a private list owned by another user.", "list.is.private", HttpStatus.UNAUTHORIZED);
  }
}
