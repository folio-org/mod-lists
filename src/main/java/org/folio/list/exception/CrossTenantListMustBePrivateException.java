package org.folio.list.exception;

import org.folio.list.services.ListActions;
import org.folio.querytool.domain.dto.EntityType;
import org.springframework.http.HttpStatus;

public class CrossTenantListMustBePrivateException extends SimpleListException {

  public CrossTenantListMustBePrivateException(EntityType entityType, ListActions failedAction) {
    super(
      null,
      failedAction,
      "This list must be private as entity type %s allows cross-tenant queries.".formatted(entityType.getName()),
      "list.is.cross-tenant.and.not.private",
      HttpStatus.BAD_REQUEST
    );
  }
}
