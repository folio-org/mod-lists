package org.folio.list.exception;

import org.folio.list.domain.dto.ListAppError;
import org.folio.list.domain.dto.Parameter;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ExportCancelledException extends AbstractListException {
  private static final String ERROR_CODE = "export.cancelled";

  private final UUID listId;
  private final UUID exportId;
  private final ListActions failedAction;

  public ExportCancelledException(UUID listId, UUID exportId, ListActions failedAction) {
    this.listId = listId;
    this.exportId = exportId;
    this.failedAction = failedAction;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getMessage() {
    return "Export " + exportId + " for list " + listId + " has been cancelled";
  }

  @Override
  public ListAppError getError() {
    return new ListAppError()
      .code(getErrorCode(failedAction, ERROR_CODE))
      .message(getMessage())
      .addParametersItem(new Parameter().key("listId").value(listId.toString()))
      .addParametersItem(new Parameter().key("exportId").value(exportId.toString()));
  }
}
