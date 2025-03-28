package org.folio.list.services.export;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.list.exception.ExportNotFoundException.exportNotFound;
import static org.folio.list.services.export.ExportUtils.getFileName;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.list.domain.AsyncProcessStatus;
import org.folio.list.domain.ExportDetails;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.dto.ListExportDTO;
import org.folio.list.exception.ExportCancelledException;
import org.folio.list.exception.ListNotFoundException;
import org.folio.list.mapper.ListExportMapper;
import org.folio.list.repository.ListExportRepository;
import org.folio.list.repository.ListRepository;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.services.AppShutdownService;
import org.folio.list.services.AppShutdownService.ShutdownTask;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListValidationService;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.querytool.domain.dto.Field;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Lazy // Do not connect to S3 when the application starts
@Service
@Log4j2
@RequiredArgsConstructor
public class ListExportService {

  private final FolioExecutionContext executionContext;
  private final SystemUserScopedExecutionService systemUserScopedExecutionService;
  private final ListExportRepository listExportRepository;
  private final ListExportMapper listExportMapper;
  private final ListRepository listRepository;
  private final ListExportWorkerService listExportWorkerService;
  private final FolioS3Client folioS3Client;
  private final ListValidationService validationService;
  private final AppShutdownService appShutdownService;
  private final EntityTypeClient entityTypeClient;

  @Transactional
  public ListExportDTO createExport(UUID listId, List<String> fields) {
    ListEntity list = listRepository
      .findByIdAndIsDeletedFalse(listId)
      .orElseThrow(() -> new ListNotFoundException(listId, ListActions.EXPORT));
    validationService.validateCreateExport(list);
    List<String> exportFields = isEmpty(fields) ? list.getFields() : fields;
    List<EntityTypeColumn> columns = entityTypeClient
      .getEntityType(list.getEntityTypeId(), ListActions.EXPORT)
      .getColumns();

    // Ensure id columns are included in export
    Set<String> idColumnNames = columns
      .stream()
      .filter(column -> Boolean.TRUE.equals(column.getIsIdColumn()))
      .map(Field::getName)
      .collect(Collectors.toSet());
    exportFields.addAll(idColumnNames);

    // Remove any fields that are not present in the entity type definition
    Set<String> columnNames = columns
      .stream()
      .map(Field::getName)
      .collect(Collectors.toSet());
    List<String> validExportFields = exportFields
      .stream()
      .filter(columnNames::contains)
      .distinct()
      .toList();

    ExportDetails exportDetails = createExportDetails(list, validExportFields);
    ExportDetails savedExport = listExportRepository.save(exportDetails);
    doAsyncExport(savedExport);
    return listExportMapper.toListExportDTO(savedExport);
  }

  public ListExportDTO getExportDetails(UUID listId, UUID exportId) {
    ExportDetails exportDetails = listExportRepository
      .findByListIdAndExportId(listId, exportId)
      .orElseThrow(() -> exportNotFound(listId, exportId, ListActions.EXPORT));
    validationService.validateExport(exportDetails.getList());
    return listExportMapper.toListExportDTO(exportDetails);
  }

  /**
   * This method downloads a list export.
   *
   * @param listId   id for the list.
   * @param exportId id for the list export.
   * @return a {@link ExportDownloadContents} with list name, download stream, and length
   */
  public ExportDownloadContents downloadExport(UUID listId, UUID exportId) {
    String fileName = getFileName(executionContext.getTenantId(), exportId);
    ListEntity list = listExportRepository
      .findByListIdAndExportId(listId, exportId)
      .map(ExportDetails::getList)
      .orElseThrow(() -> exportNotFound(listId, exportId, ListActions.EXPORT));
    validationService.validateExport(list);

    return new ExportDownloadContents(
      list.getName(),
      folioS3Client.read(fileName),
      folioS3Client.getSize(fileName)
    );
  }

  @Transactional
  public void cancelExport(UUID listId, UUID exportId) {
    log.info("Cancelling export: listId {}, exportId {}", listId, exportId);
    ExportDetails exportDetails = listExportRepository
      .findByListIdAndExportId(listId, exportId)
      .orElseThrow(() -> exportNotFound(listId, exportId, ListActions.CANCEL_EXPORT));
    validationService.validateCancelExport(exportDetails);
    exportDetails.setStatus(AsyncProcessStatus.CANCELLED);
    listExportRepository.save(exportDetails);
  }

  private ExportDetails createExportDetails(ListEntity list, List<String> fields) {
    return new ExportDetails(
      UUID.randomUUID(),
      list,
      fields,
      AsyncProcessStatus.IN_PROGRESS,
      executionContext.getUserId(),
      OffsetDateTime.now(),
      null
    );
  }

  private void doAsyncExport(ExportDetails exportDetails) {
    Runnable cancelExport = () -> cancelExport(exportDetails.getList().getId(), exportDetails.getExportId());
    ShutdownTask shutdownTask = appShutdownService.registerShutdownTask(
      executionContext,
      cancelExport,
      "Cancel export for list " + exportDetails.getList().getId()
    );
    UUID userId = executionContext.getUserId();
    log.debug("Using user {} as proxy user for export", userId);
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(
      executionContext.getTenantId(),
      () ->
        listExportWorkerService
          .doAsyncExport(exportDetails, userId)
          .whenComplete((success, throwable) -> {
            // Reassign the task (an AutoCloseable) here, to auto-close it when the export is done
            try (ShutdownTask autoClose = shutdownTask) {
              setExportStatus(exportDetails, throwable);
              exportDetails.setEndDate(OffsetDateTime.now());
              listExportRepository.save(exportDetails);
            }
          })
    );
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

  public record ExportDownloadContents(String listName, InputStream stream, long length) {}
}
