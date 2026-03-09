package org.folio.list.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;
import org.folio.list.exception.EntityTypeNotFoundException;
import org.folio.list.exception.InsufficientEntityTypePermissionsException;
import org.folio.list.services.ListActions;
import org.folio.querytool.domain.dto.ColumnValues;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.UpdateUsedByRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;

@HttpExchange(url = "entity-types")
public interface EntityTypeClient {
  @GetExchange("")
  EntityTypeSummaryResponse getEntityTypeSummary(@RequestParam(required = false) List<UUID> ids);

  @GetExchange("/{entityTypeId}")
  EntityType getEntityType(@PathVariable UUID entityTypeId);

  @GetExchange("/{entityTypeId}")
  EntityType getEntityType(@PathVariable UUID entityTypeId, @RequestParam(required = false) Boolean includeHidden);

  @GetExchange("/{entityTypeId}/columns/{columnName}/values")
  ColumnValues getColumnValues(@PathVariable UUID entityTypeId, @PathVariable String columnName);

  @PatchExchange("/{entityTypeId}/used-by")
  EntityType updateEntityTypeUsedBy(
    @PathVariable UUID entityTypeId,
    @RequestBody UpdateUsedByRequest updateUsedByRequest
  );

  /** Gets an entity type; includes wrappers for feign exceptions */
  default EntityType getEntityType(UUID entityTypeId, ListActions attemptedAction, boolean includeHidden) {
    try {
      return getEntityType(entityTypeId, includeHidden);
    } catch (HttpClientErrorException.Unauthorized e) {
      String message = e.getResponseBodyAsString();
      throw new InsufficientEntityTypePermissionsException(entityTypeId, attemptedAction, message);
    } catch (HttpClientErrorException.NotFound e) {
      throw new EntityTypeNotFoundException(entityTypeId, attemptedAction);
    }
  }

  /** Gets an entity type; includes wrappers for feign exceptions */
  default EntityType getEntityType(UUID entityTypeId, ListActions attemptedAction) {
    return getEntityType(entityTypeId, attemptedAction, false);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record EntityTypeSummary(UUID id, String label, Boolean crossTenantQueriesEnabled) {}

  record EntityTypeSummaryResponse(List<EntityTypeSummary> entityTypes, String _version) {}
}
