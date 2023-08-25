package org.folio.list.services.export;

import java.io.File;
import java.util.UUID;

public class ExportUtils {
  private static final String CSV_EXTENSION = ".csv";

  private ExportUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static String getFileName(String tenantId, UUID exportId) {
    return tenantId + File.separatorChar + exportId + CSV_EXTENSION;
  }

  public static String getFileName(UUID exportId) {
    return exportId + CSV_EXTENSION;
  }
}
