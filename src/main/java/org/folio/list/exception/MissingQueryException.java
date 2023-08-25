package org.folio.list.exception;

import org.folio.list.domain.ListEntity;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

public class MissingQueryException extends SimpleListException {
  public MissingQueryException(ListEntity entity, ListActions failedAction) {
    super(entity, failedAction, "List ( with id " + entity.getId() + " ) does not have a query defined.", "list.missing.query", HttpStatus.BAD_REQUEST);
  }
}
