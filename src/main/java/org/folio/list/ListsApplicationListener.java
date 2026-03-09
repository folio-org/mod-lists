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
    ensureS3BucketExists();
    checkS3Connection();
  }

  private void ensureS3BucketExists() {
    if (!s3CheckEnabled) {
      return;
    }

    try {
      log.info("Ensuring S3/MinIO bucket exists");
      folioS3Client.createBucketIfNotExists();
    } catch (Exception e) {
      log.error("Failed to ensure S3/MinIO bucket exists: {}", getSanitizedExceptionMessage(e));
      throw new S3ClientException("S3/MinIO bucket initialization failed.");
    }
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
    int maxUploadAttempts = 6;
    int retryDelayMs = 5000;
    int attempt = 0;
    boolean success = false;
    Exception lastException = null;

    // Bucket creation can have some propagation delay, so try multiple times before failing
    while (attempt < maxUploadAttempts && !success) {
      attempt++;
      try {
        log.info("Attempt {}: Attempting to upload test file {} to S3/MinIO to validate the config/connection", attempt, tempFilePath);
        InputStream inputStream = new ByteArrayInputStream("mod-lists test file. This can be deleted.".getBytes(StandardCharsets.UTF_8));
        folioS3Client.write(tempFilePath, inputStream);
        log.info("File uploaded successfully");
        success = true;
      } catch (Exception e) {
        lastException = e;
        log.info("Attempt {} failed to upload test file", attempt);
        log.debug("Upload exception: {}", getSanitizedExceptionMessage(e));
        if (attempt < maxUploadAttempts) {
          try {
            log.info("Retrying in {} ms...", retryDelayMs);
            Thread.sleep(retryDelayMs);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    }
    if (!success) {
      log.error("S3/MinIO configuration check failed after {} attempts: {}", maxUploadAttempts, getSanitizedExceptionMessage(lastException));
      throw new S3ClientException("S3/MinIO configuration check failed.", lastException);
    }
    try {
      folioS3Client.remove(tempFilePath);
    } catch (Exception e) {
      log.error("Unable to remove temp file from S3/MinIO (check for previous errors, as this may be because the file from was not successfully created). {}", getSanitizedExceptionMessage(e));
    }
  }
}
