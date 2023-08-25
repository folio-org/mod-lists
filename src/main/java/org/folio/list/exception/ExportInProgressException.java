package org.folio.list.exception;

import org.folio.list.domain.ListEntity;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

public class ExportInProgressException extends SimpleListException {
  public ExportInProgressException(ListEntity list, ListActions failedAction) {
    super(list, failedAction, "List ( with id " + list.getId() + " ) is currently being exported", "export.in.progress", HttpStatus.BAD_REQUEST);
  }
}
