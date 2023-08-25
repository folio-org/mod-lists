package org.folio.list.services.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.list.domain.ExportDetails;
import org.folio.list.exception.ExportCancelledException;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Service
@Lazy // Do not connect to S3 when the application starts
@RequiredArgsConstructor
@Log4j2
public class ListExportWorkerService {
  private final FolioExecutionContext folioExecutionContext;
  private final FolioS3Client folioS3Client;
  private final CsvCreator csvCreator;

  @Async
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public CompletableFuture<Boolean> doAsyncExport(ExportDetails exportDetails) {
    log.info("Starting export of list: " + exportDetails.getList().getId() + " with Export ID: " + exportDetails.getExportId());
    try (ExportLocalStorage exportLocalStorage = csvCreator.createCSV(exportDetails)) {
      log.info("Generated CSV file for exportID {}. Uploading to S3", exportDetails.getExportId());
      String destinationFileName = ExportUtils.getFileName(folioExecutionContext.getTenantId(), exportDetails.getExportId());
      folioS3Client.upload(exportLocalStorage.getAbsolutePath(), destinationFileName);
      log.info("S3 upload complete for exportId {}", exportDetails.getExportId());
      return CompletableFuture.completedFuture(true);
    } catch (ExportCancelledException ex) {
      log.info("Export {} for list {} has been cancelled", exportDetails.getExportId(), exportDetails.getList().getId());
      return CompletableFuture.failedFuture(ex);
    } catch (Exception ex) {
      log.error("Cannot complete the export for the list: " + exportDetails.getList().getId() +
        " with export Id: " + exportDetails.getExportId(), ex);
      return CompletableFuture.failedFuture(ex);
    }
  }
}
