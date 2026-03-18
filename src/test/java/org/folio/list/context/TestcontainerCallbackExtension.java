package org.folio.list.context;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@Component
@Profile("db-test")
public class TestcontainerCallbackExtension implements BeforeAllCallback {
  private static final int POSTGRES_PORT = 5432;

  @Container
  public static PostgreSQLContainer<?> dbContainer = new PostgreSQLContainer<>("postgres:12-alpine");

  @Override
  public void beforeAll(ExtensionContext context) {
    dbContainer.start();
    System.setProperty("DB_HOST", dbContainer.getHost());
    System.setProperty("DB_PORT", "" + dbContainer.getMappedPort(POSTGRES_PORT));
    System.setProperty("DB_DATABASE", dbContainer.getDatabaseName());
    System.setProperty("DB_USERNAME", dbContainer.getUsername());
    System.setProperty("DB_PASSWORD", dbContainer.getPassword());
  }

  public void afterAll(ExtensionContext context) {
    // do nothing, Testcontainers handles container shutdown
  }
}
