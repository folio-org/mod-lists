package org.folio.list.exception;

import org.folio.list.domain.ListEntity;
import org.folio.list.domain.dto.ListAppError;
import org.folio.list.domain.dto.Parameter;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class RefreshInProgressDuringShutdownException extends AbstractListException {
  private static final String ERROR_CODE = "list.refresh.in.progress.during.shutdown";

  private final UUID listId;
  private final String listName;
  private final UUID inProgressRefreshId;

  public RefreshInProgressDuringShutdownException(ListEntity list) {
    this.listId = list.getId();
    this.listName = list.getName();
    this.inProgressRefreshId = list.getInProgressRefresh().getId();
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.SERVICE_UNAVAILABLE;
  }

  @Override
  public ListAppError getError() {
    return new ListAppError()
      .code(ERROR_CODE)
      .message("Refresh failed due to shutdown")
      .addParametersItem(new Parameter().key("listId").value(listId.toString()))
      .addParametersItem(new Parameter().key("name").value(listName))
      .addParametersItem(new Parameter().key("refreshId").value(inProgressRefreshId.toString()));
  }
}
