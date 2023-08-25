package org.folio.list.exception;

import org.folio.list.domain.ListEntity;
import org.folio.list.domain.dto.ListAppError;
import org.folio.list.domain.dto.Parameter;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OptimisticLockException extends AbstractListException {
  private static final String ERROR_CODE = "optimistic.lock.exception";

  private final UUID listId;
  private final String listName;
  private final int listVersion;
  private final int versionInRequest;
  private final ListActions failedAction;

  public OptimisticLockException(ListEntity list, int versionInRequest) {
    this.listId = list.getId();
    this.listName = list.getName();
    this.listVersion = list.getVersion();
    this.versionInRequest = versionInRequest;
    this.failedAction = ListActions.UPDATE;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.CONFLICT;
  }

  @Override
  public String getMessage() {
    return "List ( with id " + listId + " ) cannot be updated since version in " +
      "request (" + versionInRequest + ") is different from the version of list (" + listVersion + ")";
  }

  @Override
  public ListAppError getError() {
    return new ListAppError()
      .code(getErrorCode(failedAction, ERROR_CODE))
      .message(getMessage())
      .addParametersItem(new Parameter().key("id").value(listId.toString()))
      .addParametersItem(new Parameter().key("name").value(listName))
      .addParametersItem(new Parameter().key("currentVersion").value(String.valueOf(listVersion)))
      .addParametersItem(new Parameter().key("requestVersion").value(String.valueOf(versionInRequest)));
  }
}
