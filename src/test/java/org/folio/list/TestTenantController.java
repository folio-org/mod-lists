package org.folio.list;

import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.tenant.rest.resource.TenantApi;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController("folioTenantController")
@Profile("test")
public class TestTenantController implements TenantApi {

  @Override
  public ResponseEntity<Void> deleteTenant(String operationId) {
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> postTenant(@Valid TenantAttributes tenantAttributes) {
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}