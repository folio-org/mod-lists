package org.folio.list.exception;

import org.folio.list.domain.dto.ListAppError;
import org.folio.list.domain.dto.Parameter;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

public class InsufficientEntityTypePermissionsException extends AbstractListException {
  private static final String ERROR_CODE = "entity.type.restricted";
  private final ListActions failedAction;
  private final UUID entityTypeId;
  private final String message;

  public InsufficientEntityTypePermissionsException(UUID entityTypeId, ListActions failedAction, String errorMessage) {
    this.entityTypeId = entityTypeId;
    this.failedAction = failedAction;
    int beginIndex = errorMessage.indexOf("User is missing permissions");
    int endIndex = errorMessage.indexOf("\"}]");
    this.message = endIndex == -1 ? errorMessage.substring(beginIndex) : errorMessage.substring(beginIndex, endIndex);
  }


  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.FORBIDDEN;
  }

  @Override
  public String getMessage() {
    return "User is missing permissions to access entity type " + entityTypeId + ". " + message;
  }

  @Override
  public ListAppError getError() {
    return new ListAppError()
      .code(getErrorCode(failedAction, ERROR_CODE))
      .message(getMessage())
      .parameters(List.of(
        new Parameter().key("entityTypeId").value(entityTypeId.toString())
      ));
  }
}
