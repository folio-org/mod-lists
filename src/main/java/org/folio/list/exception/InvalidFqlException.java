package org.folio.list.exception;

import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;
import org.folio.list.domain.dto.ListAppError;
import org.folio.list.domain.dto.Parameter;

import java.util.Map;

public class InvalidFqlException extends AbstractListException {
  private static final String ERROR_CODE = "fql.query.invalid";

  private final String fqlQuery;
  private final ListActions failedAction;
  private final Map<String, String> errors;

  public InvalidFqlException(String fqlQuery, ListActions failedAction, Map<String, String> errors) {
    this.fqlQuery = fqlQuery;
    this.failedAction = failedAction;
    this.errors = errors;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getMessage() {
    return "FQL Query \"" + fqlQuery + "\" is invalid";
  }

  @Override
  public ListAppError getError() {
    var listAppError = new ListAppError()
      .code(getErrorCode(failedAction, ERROR_CODE))
      .message(getMessage());
    errors.forEach((key, value) -> listAppError.addParametersItem(new Parameter().key(key).value(value)));
    return listAppError;
  }
}
