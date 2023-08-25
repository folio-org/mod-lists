package org.folio.list.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.folio.querytool.domain.dto.EntityType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "entity-types")
public interface EntityTypeClient {
  @GetMapping("")
  List<EntityTypeSummary> getEntityTypeSummary(@RequestParam List<UUID> ids);

  @GetMapping("/{entityTypeId}")
  EntityType getEntityType(@RequestHeader UUID entityTypeId);

  @JsonIgnoreProperties(ignoreUnknown = true)
  record EntityTypeSummary(UUID id, String label) {}
}
