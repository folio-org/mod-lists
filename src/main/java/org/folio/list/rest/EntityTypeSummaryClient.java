package org.folio.list.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "entity-types")
public interface EntityTypeSummaryClient {
  @GetMapping("")
  List<EntityTypeSummary> getEntityTypeSummary(@RequestParam List<UUID> ids);

  @JsonIgnoreProperties(ignoreUnknown = true)
  record EntityTypeSummary(UUID id, String label) {}
}
