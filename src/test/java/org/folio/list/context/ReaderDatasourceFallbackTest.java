package org.folio.list.context;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ActiveProfiles("db-test")
@SpringBootTest
@ExtendWith(TestcontainerCallbackExtension.class)
class ReaderDatasourceFallbackTest {
  @Autowired
  @Qualifier("readerDataSource")
  private DataSource readerDatasource;

  @Autowired
  private DataSource writerDatasource;

  @Test
  @Disabled("This feature is going away soon. In the meantime, this test failing only on Jenkins and breaking builds")
  void readerShouldFallbackToWriterDbWhenReaderNotAvailable() throws SQLException {
    assertNotNull(System.getProperty("DB_HOST"));
    assertNotNull(System.getProperty("DB_PORT"));
    assertNotNull(System.getProperty("DB_DATABASE"));
    assertNull(System.getProperty("DB_HOST_READER"));

    String expectedDbUrl = "jdbc:postgresql://" + System.getProperty("DB_HOST")
      + ":" + System.getProperty("DB_PORT") + "/" + System.getProperty("DB_DATABASE");

    assertEquals(expectedDbUrl, readerDatasource.getConnection().getMetaData().getURL());
    assertEquals(expectedDbUrl, writerDatasource.getConnection().getMetaData().getURL());
  }
}
