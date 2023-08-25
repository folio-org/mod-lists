package org.folio.list.exception;

import org.folio.list.domain.ListEntity;
import org.folio.list.services.ListActions;
import org.folio.list.domain.dto.ListAppError;
import org.folio.list.domain.dto.Parameter;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public abstract class SimpleListException extends AbstractListException {
  private final UUID listId;
  private final String listName;
  private final ListActions failedAction;
  private final String errorMessage;
  private final String errorCode;
  private final HttpStatus httpStatus;

  protected SimpleListException(ListEntity list, ListActions failedAction, String errorMessage, String errorCode, HttpStatus httpStatus) {
    this.listId = list.getId();
    this.listName = list.getName();
    this.failedAction = failedAction;
    this.errorMessage = errorMessage;
    this.errorCode = errorCode;
    this.httpStatus = httpStatus;
  }

  @Override
  public final String getMessage() {
    return errorMessage;
  }

  @Override
  public final ListAppError getError() {
    return new org.folio.list.domain.dto.ListAppError()
      .code(getErrorCode(failedAction, errorCode))
      .message(getMessage())
      .addParametersItem(new Parameter().key("id").value(listId.toString()))
      .addParametersItem(new Parameter().key("name").value(listName));
  }

  @Override
  public final HttpStatus getHttpStatus() {
    return httpStatus;
  }

}
