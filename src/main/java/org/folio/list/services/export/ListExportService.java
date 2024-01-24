package org.folio.list.services.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.list.domain.AsyncProcessStatus;
import org.folio.list.domain.ExportDetails;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.dto.ListExportDTO;
import org.folio.list.exception.ExportCancelledException;
import org.folio.list.exception.ListNotFoundException;
import org.folio.list.mapper.ListExportMapper;
import org.folio.list.repository.ListExportRepository;
import org.folio.list.repository.ListRepository;
import org.folio.list.services.ListActions;
import org.folio.list.services.AppShutdownService;
import org.folio.list.services.ListValidationService;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.folio.list.services.export.ExportUtils.getFileName;
import static org.folio.list.exception.ExportNotFoundException.exportNotFound;

@Lazy // Do not connect to S3 when the application starts
@Service
@Log4j2
@RequiredArgsConstructor
public class ListExportService {
  private final FolioExecutionContext executionContext;
  private final ListExportRepository listExportRepository;
  private final ListExportMapper listExportMapper;
  private final ListRepository listRepository;
  private final ListExportWorkerService listExportWorkerService;
  private final FolioS3Client folioS3Client;
  private final ListValidationService validationService;
  private final AppShutdownService appShutdownService;

  @Transactional
  public ListExportDTO createExport(UUID listId) {
    ListEntity list = listRepository.findByIdAndIsDeletedFalse(listId)
      .orElseThrow(() -> new ListNotFoundException(listId, ListActions.EXPORT));
    validationService.validateCreateExport(list);
    ExportDetails exportDetails = createExportDetails(list);
    ExportDetails savedExport = listExportRepository.save(exportDetails);
    doAsyncExport(savedExport);
    return listExportMapper.toListExportDTO(savedExport);
  }

  public ListExportDTO getExportDetails(UUID listId, UUID exportId) {
    ExportDetails exportDetails = listExportRepository.findByListIdAndExportId(listId, exportId)
      .orElseThrow(() -> exportNotFound(listId, exportId, ListActions.EXPORT));
    validationService.validateGetExport(exportDetails.getList());
    return listExportMapper.toListExportDTO(exportDetails);
  }

  /**
   * This method downloads a list export.
   *
   * @param listId   id for the list.
   * @param exportId id for the list export.
   * @return a pair containing String and InputStream.
   * The String contains the listName and the InputStream streams the CSV contents of the list.
   */
  public Pair<String, InputStream> downloadExport(UUID listId, UUID exportId) {
    String fileName = getFileName(executionContext.getTenantId(), exportId);
    ListEntity list = listExportRepository.findByListIdAndExportId(listId, exportId)
      .map(ExportDetails::getList)
      .orElseThrow(() -> exportNotFound(listId, exportId, ListActions.EXPORT));
    validationService.validateDownloadExport(list);
    InputStream csvStream = folioS3Client.read(fileName);
    return Pair.of(list.getName(), csvStream);
  }

  @Transactional
  public void cancelExport(UUID listId, UUID exportId) {
    log.info("Cancelling export: listId {}, exportId {}", listId, exportId);
    ExportDetails exportDetails = listExportRepository.findByListIdAndExportId(listId, exportId)
      .orElseThrow(() -> exportNotFound(listId, exportId, ListActions.CANCEL_EXPORT));
    validationService.validateCancelExport(exportDetails);
    exportDetails.setStatus(AsyncProcessStatus.CANCELLED);
    listExportRepository.save(exportDetails);
  }

  private ExportDetails createExportDetails(ListEntity list) {
    return new ExportDetails(
      UUID.randomUUID(),
      list,
      AsyncProcessStatus.IN_PROGRESS,
      executionContext.getUserId(),
      OffsetDateTime.now(),
      null
    );
  }

  private void doAsyncExport(ExportDetails exportDetails) {
    Runnable cancelExport = () -> cancelExport(exportDetails.getList().getId(), exportDetails.getExportId());
    var shutdownTask = appShutdownService.registerShutdownTask(executionContext, cancelExport, "Cancel export for list " + exportDetails.getList().getId());
    listExportWorkerService.doAsyncExport(exportDetails)
      .whenComplete((success, throwable) -> {
        try (var autoClose = shutdownTask) { // Reassign the task (an AutoCloseable) here, to auto-close it when the export is done
          setExportStatus(exportDetails, throwable);
          exportDetails.setEndDate(OffsetDateTime.now());
          listExportRepository.save(exportDetails);
        }
      });
  }

  private void setExportStatus(ExportDetails exportDetails, Throwable throwable) {
    if (throwable == null) {
      exportDetails.setStatus(AsyncProcessStatus.SUCCESS);
    } else if (throwable.getCause() instanceof ExportCancelledException) {
      exportDetails.setStatus(AsyncProcessStatus.CANCELLED);
    } else {
      exportDetails.setStatus(AsyncProcessStatus.FAILED);
    }
  }
}
