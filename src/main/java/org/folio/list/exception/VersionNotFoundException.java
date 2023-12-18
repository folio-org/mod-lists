package org.folio.list.exception;

import java.util.UUID;
import org.folio.list.domain.dto.ListAppError;
import org.folio.list.domain.dto.Parameter;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

public class VersionNotFoundException extends AbstractListException {

  private static final String ERROR_CODE = "version.not.found";
  private static final String ERROR_MESSAGE_TEMPLATE =
    "List (id=%s) does not have a version number %d";

  private final UUID listId;
  private final int versionNumber;
  private final ListActions failedAction;
  private final HttpStatus httpStatus;

  public VersionNotFoundException(
    UUID listId,
    int versionNumber,
    ListActions failedAction
  ) {
    this(listId, versionNumber, failedAction, HttpStatus.NOT_FOUND);
  }

  public VersionNotFoundException(
    UUID listId,
    int versionNumber,
    ListActions failedAction,
    HttpStatus httpStatus
  ) {
    this.listId = listId;
    this.versionNumber = versionNumber;
    this.httpStatus = httpStatus;
    this.failedAction = failedAction;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  @Override
  public String getMessage() {
    return String.format(ERROR_MESSAGE_TEMPLATE, listId, versionNumber);
  }

  @Override
  public ListAppError getError() {
    return new ListAppError()
      .code(getErrorCode(failedAction, ERROR_CODE))
      .message(getMessage())
      .addParametersItem(new Parameter().key("id").value(listId.toString()))
      .addParametersItem(
        new Parameter()
          .key("versionNumber")
          .value(Integer.toString(versionNumber))
      );
  }
}
