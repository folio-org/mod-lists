package org.folio.list.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import feign.FeignException;
import java.util.List;
import java.util.UUID;

import org.folio.list.exception.EntityTypeNotFoundException;
import org.folio.list.exception.InsufficientEntityTypePermissionsException;
import org.folio.list.services.ListActions;
import org.folio.querytool.domain.dto.ColumnValues;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.UpdateUsedByRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "entity-types")
public interface EntityTypeClient {
  @GetMapping("")
  EntityTypeSummaryResponse getEntityTypeSummary(@RequestParam List<UUID> ids);

  @GetMapping("/{entityTypeId}")
  EntityType getEntityType(@RequestHeader UUID entityTypeId);

  @GetMapping("/{entityTypeId}")
  EntityType getEntityType(@RequestHeader UUID entityTypeId, @RequestParam boolean includeHidden);

  @GetMapping("/{entityTypeId}/columns/{columnName}/values" )
  ColumnValues getColumnValues(@RequestHeader UUID entityTypeId, @RequestHeader String columnName);

  @PatchMapping("/{entityTypeId}/used-by")
  EntityType updateEntityTypeUsedBy(@RequestHeader UUID entityTypeId, @RequestBody UpdateUsedByRequest updateUsedByRequest);

  /** Gets an entity type; includes wrappers for feign exceptions */
  default EntityType getEntityType(UUID entityTypeId, ListActions attemptedAction, boolean includeHidden) {
    try {
      return getEntityType(entityTypeId, includeHidden);
    } catch (FeignException.Unauthorized e) {
      String message = e.getMessage();
      throw new InsufficientEntityTypePermissionsException(entityTypeId, attemptedAction, message);
    } catch (FeignException.NotFound e) {
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
