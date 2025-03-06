package org.folio.list;

import lombok.extern.log4j.Log4j2;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.exception.S3ClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.folio.list.util.LogUtils.getSanitizedExceptionMessage;

@Log4j2
@Component
public class ListsApplicationListener implements ApplicationListener<ApplicationReadyEvent> {

  @Value("${mod-lists.list-export.s3-startup-check.enabled:true}")
  private boolean s3CheckEnabled;

  private final FolioS3Client folioS3Client;

  @Autowired
  public ListsApplicationListener(FolioS3Client folioS3Client) {
    this.folioS3Client = folioS3Client;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    checkS3Connection();
  }

  private void checkS3Connection() {
    if (!s3CheckEnabled) {
      log.info("Skipping S3/MinIO connection check");
      return;
    }

    log.info("Checking S3/MinIO connection");
    // Use the current date/time, to make it easier to track down files if something goes wrong, and add a UUID to ensure uniqueness
    var formatter = DateTimeFormatter.ofPattern("yyyyMMdd-kkmmssSS"); // 20240412-14475478 = April 12, 2024 14:47:54.78
    var now = LocalDateTime.now();
    String tempFilePath = "mod-lists-s3-test-tmp-" + now.format(formatter) + '-' + UUID.randomUUID();

    // Try uploading and deleting a temp file
    try {
      log.info("Attempting to upload test file %s to S3/MinIO to validate the config/connection".formatted(tempFilePath));
      InputStream inputStream = new ByteArrayInputStream("mod-lists test file. This can be deleted.".getBytes(StandardCharsets.UTF_8));
      folioS3Client.write(tempFilePath, inputStream);
      log.info("File uploaded successfully");
    } catch (Exception e) {
      log.error("S3/MinIO configuration check failed: {}", getSanitizedExceptionMessage(e));
      throw new S3ClientException("S3/MinIO configuration check failed.");
    } finally {
      try {
        folioS3Client.remove(tempFilePath);
      } catch (Exception e) {
        // Don't throw anything here, since deleting isn't actually all that important for mod-lists
        // If there truly was a fatal error, it'll get thrown in the previous try block
        log.error("Unable to remove temp file from S3/MinIO (check for previous errors, as this may be because the file from was not successfully created). {}", getSanitizedExceptionMessage(e));
      }
    }
  }
}
