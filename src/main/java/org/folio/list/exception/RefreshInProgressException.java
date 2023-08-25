package org.folio.list.exception;

import org.folio.list.domain.dto.ListAppError;
import org.folio.list.domain.dto.Parameter;
import org.folio.list.domain.ListEntity;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class RefreshInProgressException extends AbstractListException {
  private static final String ERROR_CODE = "list.refresh.in.progress";

  private final UUID listId;
  private final String listName;
  private final UUID inProgressRefreshId;
  private final ListActions failedAction;

  public RefreshInProgressException(ListEntity list, ListActions failedAction) {
    this.listId = list.getId();
    this.listName = list.getName();
    this.inProgressRefreshId = list.getInProgressRefresh().getId();
    this.failedAction = failedAction;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getMessage() {
    return "List ( with id " + listId + " ) is already in refresh state";
  }

  @Override
  public ListAppError getError() {
    return new ListAppError()
      .code(getErrorCode(failedAction, ERROR_CODE))
      .message(getMessage())
      .addParametersItem(new Parameter().key("listId").value(listId.toString()))
      .addParametersItem(new Parameter().key("name").value(listName))
      .addParametersItem(new Parameter().key("refreshId").value(inProgressRefreshId.toString()));
  }
}
