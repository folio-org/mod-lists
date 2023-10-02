package org.folio.list.context;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.SQLException;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@SpringBootTest
class DatasourceConnectionTest {

  @Container
  public static PostgreSQLContainer<?> writerDbContainer = new PostgreSQLContainer<>("postgres:12-alpine");

  @Autowired
  private DataSource writerDatasource;

  @BeforeAll
  static void setup() {
    int postgresPort = 5432;
    setProperty("DB_DATABASE", writerDbContainer.getDatabaseName());
    setProperty("DB_USERNAME", writerDbContainer.getUsername());
    setProperty("DB_PASSWORD", writerDbContainer.getPassword());
    setProperty("DB_HOST", writerDbContainer.getHost());
    setProperty("DB_PORT", "" + writerDbContainer.getMappedPort(postgresPort));
  }

  @Test
  void testDbConnection() throws SQLException {
    assertNotNull(getProperty("DB_HOST"));
    assertNotNull(getProperty("DB_PORT"));
    assertNotNull(getProperty("DB_DATABASE"));

    String dbUrlFormat = "jdbc:postgresql://%s:%s/%s";
    String expectedWriterDbUrl = format(dbUrlFormat, getProperty("DB_HOST"), getProperty("DB_PORT"),
      getProperty("DB_DATABASE"));

    assertEquals(expectedWriterDbUrl, writerDatasource.getConnection().getMetaData().getURL());
  }
}
