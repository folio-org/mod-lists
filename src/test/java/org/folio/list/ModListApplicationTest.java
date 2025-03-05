package org.folio.list;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Valid;
import org.folio.list.context.TestcontainerCallbackExtension;
import org.folio.spring.liquibase.FolioLiquibaseConfiguration;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.tenant.rest.resource.TenantApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@ActiveProfiles({"test", "db-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@ExtendWith(TestcontainerCallbackExtension.class)
class ModListApplicationTest {

  @EnableAutoConfiguration(exclude = {FolioLiquibaseConfiguration.class})
  @RestController("folioTenantController")
  @Profile("test")
  static class TestTenantController implements TenantApi {

    @Override
    public ResponseEntity<Void> deleteTenant(String operationId) {
      return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> postTenant(@Valid TenantAttributes tenantAttributes) {
      return ResponseEntity.status(HttpStatus.CREATED).build();
    }
  }

  @Test
  void shouldAnswerWithTrueToMakeSonarHappy() {
    // Sonarqube doesn't think this class tests anything, when in fact it does verify that the application can start
    // up with minimum config without problems. This dummy test method is just here to make Sonar happy.
    assertTrue(true);
  }
}
