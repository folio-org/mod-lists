package org.folio.list.services.export;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.UUID;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.size;

@Log4j2
public class ExportLocalStorage implements AutoCloseable {
  private static final String CSV_EXTENSION = ".csv";

  private final Path localFile;

  @SneakyThrows
  public ExportLocalStorage(UUID exportId) {
    FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));
    localFile = createTempFile(ExportUtils.getFileName(exportId), CSV_EXTENSION, attr);
    log.info("Created local file for export {}. Path: {}", exportId, localFile.getFileName());
  }

  @SneakyThrows
  public OutputStream outputStream() {
    return new FileOutputStream(localFile.toFile(), true);
  }

  @SneakyThrows
  public InputStream inputStream() {
    log.info("Returning contents from local storage. Size: {} kb", (size(localFile) / 1024));
    return new FileInputStream(localFile.toFile());
  }

  public String getAbsolutePath() {
    return localFile.toFile().getAbsolutePath();
  }

  @Override
  public void close() {
    boolean deleted = FileUtils.deleteQuietly(localFile.toFile());
    if (deleted) {
      log.info("Local file deleted {}", localFile.getFileName());
    } else {
      log.error("Failed to delete file {}", localFile.getFileName());
    }
  }
}
