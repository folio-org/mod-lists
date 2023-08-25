package org.folio.list.service.export;

import org.folio.list.services.export.ExportLocalStorage;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExportLocalStorageTest {
  @Test
  void exportLocalStorageTest() throws Exception {
    UUID exportId = UUID.randomUUID();
    String line1 = "Line 1";
    String line2 = "Line 2";
    String expectedFileContent1 = """
      Line 1""";
    String expectedFileContent2 = """
      Line 1Line 2""";
    ExportLocalStorage exportLocalStorage = new ExportLocalStorage(exportId);
    exportLocalStorage.outputStream().write(line1.getBytes());
    String actualFileContent1 = new String(exportLocalStorage.inputStream().readAllBytes());
    assertEquals(expectedFileContent1, actualFileContent1);

    exportLocalStorage.outputStream().write(line2.getBytes());
    String actualFileContent2 = new String(exportLocalStorage.inputStream().readAllBytes());
    assertEquals(expectedFileContent2, actualFileContent2);

    exportLocalStorage.close();
    assertThrows(Exception.class, exportLocalStorage::inputStream);
  }
}
