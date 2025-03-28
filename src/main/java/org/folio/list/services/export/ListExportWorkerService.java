package org.folio.list.services.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.list.domain.ExportDetails;
import org.folio.list.exception.ExportCancelledException;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.exception.S3ClientException;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.folio.list.util.LogUtils.getSanitizedExceptionMessage;

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
  public CompletableFuture<Boolean> doAsyncExport(ExportDetails exportDetails, UUID userId) {
    log.info("Starting export of list: " + exportDetails.getList().getId() + " with Export ID: " + exportDetails.getExportId());
    String destinationFileName = ExportUtils.getFileName(folioExecutionContext.getTenantId(), exportDetails.getExportId());
    String uploadId = null;
    var partETags = new ArrayList<String>();
    try {
      uploadId = folioS3Client.initiateMultipartUpload(destinationFileName);
      log.info("S3 multipart upload initialized for exportId {}", exportDetails.getExportId());

      ExportLocalStorage andUploadCSV = csvCreator.createAndUploadCSV(exportDetails, destinationFileName, uploadId, partETags, userId);
      andUploadCSV.close();

      folioS3Client.completeMultipartUpload(destinationFileName, uploadId, partETags);
      log.info("S3 multipart upload complete for exportId {}", exportDetails.getExportId());
      return CompletableFuture.completedFuture(true);
    } catch (ExportCancelledException ex) {
      log.info("Export {} for list {} has been cancelled", exportDetails.getExportId(), exportDetails.getList().getId());
      abortMultipartUpload(destinationFileName, uploadId, exportDetails);
      return CompletableFuture.failedFuture(ex);
    } catch (Exception ex) {

      log.error("Cannot complete the export for the list: {} with exportId: {}. Failure reason: {}",
        exportDetails.getList().getId(),
        exportDetails.getExportId(),
        getSanitizedExceptionMessage(ex)
      );
      abortMultipartUpload(destinationFileName, uploadId, exportDetails);
      return CompletableFuture.failedFuture(new S3ClientException("S3 upload failed"));
    }
  }

  private void abortMultipartUpload(String destinationFileName, String uploadId, ExportDetails exportDetails) {
    if (uploadId != null) {
      log.info("Abort s3 multipart upload for exportId {}", exportDetails.getExportId());
      folioS3Client.abortMultipartUpload(destinationFileName, uploadId);
    }
  }
}
