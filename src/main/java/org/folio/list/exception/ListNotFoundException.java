package org.folio.list.exception;

import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;
import org.folio.list.domain.dto.ListAppError;
import org.folio.list.domain.dto.Parameter;

import java.util.UUID;

public class ListNotFoundException extends AbstractListException {
  private static final String ERROR_CODE = "list.not.found";

  private final UUID listId;
  private final ListActions failedAction;
  private final HttpStatus httpStatus;

  public ListNotFoundException(UUID listId, ListActions failedAction) {
    this(listId, failedAction, HttpStatus.NOT_FOUND);
  }

  public ListNotFoundException(UUID listId, ListActions failedAction, HttpStatus httpStatus) {
    this.listId = listId;
    this.httpStatus = httpStatus;
    this.failedAction = failedAction;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  @Override
  public String getMessage() {
    return "List ( with id " + listId + " ) does not exist";
  }

  @Override
  public ListAppError getError() {
    return new ListAppError()
      .code(getErrorCode(failedAction, ERROR_CODE))
      .message(getMessage())
      .addParametersItem(new Parameter().key("id").value(listId.toString()));
  }
}
