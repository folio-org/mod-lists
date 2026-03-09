package org.folio.list.rest;

import org.folio.querytool.domain.dto.FqmMigrateRequest;
import org.folio.querytool.domain.dto.FqmMigrateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "fqm")
public interface MigrationClient {
  @GetMapping("version")
  String getVersion();

  @PostMapping("migrate")
  FqmMigrateResponse migrate(@RequestBody FqmMigrateRequest request);
}
