package org.folio.list.exception;

import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;
import org.folio.list.domain.dto.ListAppError;

public abstract class AbstractListException extends RuntimeException {

  public abstract HttpStatus getHttpStatus();

  public abstract ListAppError getError();

  protected String getErrorCode(ListActions failedAction, String errorCode) {
    return failedAction.getName() + "-" + errorCode;
  }
}
