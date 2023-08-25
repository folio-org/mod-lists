package org.folio.list.exception;

import org.folio.list.domain.ListEntity;
import org.folio.list.domain.dto.ListAppError;
import org.folio.list.domain.dto.Parameter;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class RefreshCancelledException extends AbstractListException {
  private static final String ERROR_CODE = "refresh.cancelled";

  private final UUID listId;
  private final String listName;
  private final String inProgressRefreshId;
  private final ListActions failedAction;

  public RefreshCancelledException(ListEntity list) {
    this.listId = list.getId();
    this.listName = list.getName();
    this.inProgressRefreshId = list.getInProgressRefreshId()
      .map(Object::toString)
      .orElse("NULL");
    this.failedAction = ListActions.REFRESH;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getMessage() {
    return "Refresh cancelled for list with id " + listId;
  }

  @Override
  public ListAppError getError() {
    return new ListAppError()
      .code(getErrorCode(failedAction, ERROR_CODE))
      .message(getMessage())
      .addParametersItem(new Parameter().key("listId").value(listId.toString()))
      .addParametersItem(new Parameter().key("name").value(listName))
      .addParametersItem(new Parameter()
        .key("refreshId")
        .value(inProgressRefreshId)
      );
  }
}
