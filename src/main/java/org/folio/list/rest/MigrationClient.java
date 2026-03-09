package org.folio.list.rest;

import org.folio.querytool.domain.dto.FqmMigrateRequest;
import org.folio.querytool.domain.dto.FqmMigrateResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "http://localhost:8000/")
public interface MigrationClient {
  @GetExchange("test.txt")
  String getVersion();

  @PostExchange("migrate")
  FqmMigrateResponse migrate(@RequestBody FqmMigrateRequest request);
}
