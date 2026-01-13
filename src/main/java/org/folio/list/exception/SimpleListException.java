package org.folio.list.exception;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.CheckForNull;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.dto.ListAppError;
import org.folio.list.domain.dto.Parameter;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

public abstract class SimpleListException extends AbstractListException {

  @CheckForNull
  private final ListEntity list;

  private final ListActions failedAction;
  private final String errorMessage;
  private final String errorCode;
  private final HttpStatus httpStatus;

  protected SimpleListException(
    @CheckForNull ListEntity list,
    ListActions failedAction,
    String errorMessage,
    String errorCode,
    HttpStatus httpStatus
  ) {
    this.list = list;
    this.failedAction = failedAction;
    this.errorMessage = errorMessage;
    this.errorCode = errorCode;
    this.httpStatus = httpStatus;
  }

  @Override
  public final String getMessage() {
    return errorMessage;
  }

  @Override
  public final ListAppError getError() {
    return new ListAppError()
      .code(getErrorCode(failedAction, errorCode))
      .message(getMessage())
      .addParametersItem(
        new Parameter()
          .key("id")
          .value(Optional.ofNullable(list).map(ListEntity::getId).map(UUID::toString).orElse(null))
      )
      .addParametersItem(
        new Parameter().key("name").value(Optional.ofNullable(list).map(ListEntity::getName).orElse(null))
      );
  }

  @Override
  public final HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
