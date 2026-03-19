package org.folio.list.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.folio.list.context.TestcontainerCallbackExtension;
import org.folio.list.services.RunAsSystemUserService;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Log4j2
@ActiveProfiles({ "test", "db-test" })
@SpringBootTest
@ExtendWith(TestcontainerCallbackExtension.class)
class RunAsSystemUserServiceTest {

  private static final String TENANT_ID = "test1";
  private static final String INNER_TENANT_ID = "test2";
  private static final String TOKEN = "token";
  private static final String OKAPI_URL = "test_url";
  private static final String MODULE_NAME = "our-module";

  private static final FolioExecutionContext REGULAR_EXECUTION_CONTEXT = new DefaultFolioExecutionContext(
    new FolioModuleMetadata() {
      public String getModuleName() {
        return MODULE_NAME;
      }

      public String getDBSchemaName(String t) {
        return t + "_schema";
      }
    },
    Map.ofEntries(
      Map.entry(XOkapiHeaders.TENANT, Collections.singleton(TENANT_ID)),
      Map.entry(XOkapiHeaders.TOKEN, Collections.singleton(TOKEN)),
      Map.entry(XOkapiHeaders.URL, Collections.singleton(OKAPI_URL))
    )
  );

  @Autowired
  FolioExecutionContext folioExecutionContext;

  @Autowired
  RunAsSystemUserService runAsSystemUserService;

  @Test
  void testSyncExecution() {
    REGULAR_EXECUTION_CONTEXT.execute(() -> {
      verifyRegularContext();

      String result = runAsSystemUserService.executeSystemUserScoped(
        INNER_TENANT_ID,
        () -> {
          verifySystemUserContext();

          return "result";
        }
      );
      assertEquals("result", result);

      verifyRegularContext();
      return null;
    });
  }

  @Test
  void testAsyncExecution() {
    REGULAR_EXECUTION_CONTEXT.execute(() -> {
      verifyRegularContext();

      String result = runAsSystemUserService
        .executeAsyncSystemUserScoped(
          INNER_TENANT_ID,
          () -> {
            verifySystemUserContext();

            return "result";
          }
        )
        .get();
      assertEquals("result", result);

      verifyRegularContext();
      return null;
    });
  }

  private void verifyRegularContext() {
    assertEquals(OKAPI_URL, folioExecutionContext.getOkapiUrl());
    assertEquals(MODULE_NAME, folioExecutionContext.getFolioModuleMetadata().getModuleName());
    assertEquals(TENANT_ID, folioExecutionContext.getTenantId());
    assertEquals(TOKEN, folioExecutionContext.getToken());
  }

  private void verifySystemUserContext() {
    assertEquals(OKAPI_URL, folioExecutionContext.getOkapiUrl());
    assertEquals(MODULE_NAME, folioExecutionContext.getFolioModuleMetadata().getModuleName());
    assertEquals(INNER_TENANT_ID, folioExecutionContext.getTenantId());
    assertEquals("", folioExecutionContext.getToken());
  }
}
