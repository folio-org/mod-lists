package org.folio.list.exception;

import org.folio.list.domain.ListEntity;
import org.folio.list.services.ListActions;
import org.springframework.http.HttpStatus;

public class MaxListSizeExceededException extends SimpleListException {
  public MaxListSizeExceededException(ListEntity list, int maxListSize) {
    super(list, ListActions.REFRESH, "List ( with id " + list.getId() + " ) has exceeded maximum list size of " + maxListSize, "max.list.size.exceeded", HttpStatus.BAD_REQUEST);
  }
}
