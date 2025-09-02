package org.folio.list.exception;

import java.util.UUID;
import org.folio.list.domain.dto.ListAppError;
import org.folio.list.domain.dto.Parameter;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

public class EntityTypeNotFoundException extends AbstractListException {

  private static final String ERROR_CODE = "entity.not.found";

  private final UUID entityId;
  private final ListActions failedAction;
  private final HttpStatus httpStatus;

  public EntityTypeNotFoundException(UUID entityId, ListActions failedAction) {
    this(entityId, failedAction, HttpStatus.NOT_FOUND);
  }

  public EntityTypeNotFoundException(UUID entityId, ListActions failedAction, HttpStatus httpStatus) {
    this.entityId = entityId;
    this.httpStatus = httpStatus;
    this.failedAction = failedAction;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  @Override
  public String getMessage() {
    return "Entity type with ID %s does not exist.".formatted(entityId);
  }

  @Override
  public ListAppError getError() {
    return new ListAppError()
      .code(getErrorCode(failedAction, ERROR_CODE))
      .message(getMessage())
      .addParametersItem(new Parameter().key("entityTypeId").value(entityId.toString()));
  }
}
