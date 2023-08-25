package org.folio.list.exception;

import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;
import org.folio.list.domain.dto.ListAppError;
import org.folio.list.domain.dto.Parameter;

import java.util.UUID;

public class ExportNotFoundException extends AbstractListException {
  private final String errorCode;

  private final UUID listId;
  private final UUID exportId;
  private final ListActions failedAction;
  private final HttpStatus httpStatus;
  private final String message;

  public static ExportNotFoundException exportNotFound(UUID listId, UUID exportId, ListActions failedAction) {
    return new ExportNotFoundException(listId, exportId, failedAction, HttpStatus.NOT_FOUND, false);
  }

  public static ExportNotFoundException inProgressExportNotFound(UUID listId, UUID exportId, ListActions failedAction) {
    return new ExportNotFoundException(listId, exportId, failedAction, HttpStatus.NOT_FOUND, true);
  }

  private ExportNotFoundException(UUID listId, UUID exportId, ListActions failedAction, HttpStatus httpStatus, boolean notInProgress) {
    this.listId = listId;
    this.exportId = exportId;
    this.failedAction = failedAction;
    this.httpStatus = httpStatus;
    if (notInProgress) {
      this.message = "Export " + exportId + " for list " + listId + " is not in progress";
      this.errorCode = "export.not.in.progress";
    } else {
      this.message = "No export found for list id " + listId + " and export id " + exportId;
      this.errorCode = "export.not.found";
    }
  }

  @Override
  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public ListAppError getError() {
    return new ListAppError()
      .code(getErrorCode(failedAction, errorCode))
      .message(getMessage())
      .addParametersItem(new Parameter().key("listId").value(listId.toString()))
      .addParametersItem(new Parameter().key("exportId").value(exportId.toString()));
  }
}
