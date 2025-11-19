package org.folio.list.service.export;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.list.domain.AsyncProcessStatus;
import org.folio.list.domain.ExportDetails;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.dto.ListExportDTO;
import org.folio.list.exception.ExportNotFoundException;
import org.folio.list.exception.PrivateListOfAnotherUserException;
import org.folio.list.mapper.ListExportMapper;
import org.folio.list.repository.ListExportRepository;
import org.folio.list.repository.ListRepository;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.services.AppShutdownService;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListValidationService;
import org.folio.list.services.export.ExportUtils;
import org.folio.list.services.export.ListExportService;
import org.folio.list.services.export.ListExportWorkerService;
import org.folio.list.services.export.ListExportService.ExportDownloadContents;
import org.folio.list.util.TestDataFixture;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListExportServiceTest {

  private static final String TENANT_ID = "test-tenant";

  @InjectMocks
  private ListExportService listExportService;

  @Mock
  private ListRepository listRepository;

  @Mock
  private ListExportRepository listExportRepository;

  @Mock
  private FolioExecutionContext folioExecutionContext;

  @Mock
  private ListExportMapper listExportMapper;

  @Mock
  private ListExportWorkerService listExportWorkerService;

  @Mock
  private FolioS3Client folioS3Client;

  @Mock
  private ListValidationService validationService;

  @Mock
  private AppShutdownService appShutdownService;

  @Mock
  private SystemUserScopedExecutionService systemUserScopedExecutionService;

  @Mock
  private EntityTypeClient entityTypeClient;

  @Test
  void shouldSaveExport() {
    UUID listId = TestDataFixture.getListExportDetails().getList().getId();
    UUID userId = UUID.randomUUID();
    List<String> fields = new ArrayList<>(
      List.of("field1", "field2")
    );
    List<String> expectedExportFields = List.of("field1", "field2");
    List<EntityTypeColumn> entityTypeColumns =
      new ArrayList<>(
        List.of(
          new EntityTypeColumn().name("field1").isIdColumn(true),
          new EntityTypeColumn().name("field2"),
          new EntityTypeColumn().name("field3").isIdColumn(true)
        )
      );
    EntityType entityType = new EntityType()
      .name("test-entity")
      .columns(entityTypeColumns);
    ListEntity fetchedEntity = TestDataFixture.getListExportDetails().getList();
    ExportDetails exportDetails = TestDataFixture.getListExportDetails();
    ArgumentCaptor<ExportDetails> exportDetailsArgumentCaptor = ArgumentCaptor.forClass(ExportDetails.class);
    when(listRepository.findByIdAndIsDeletedFalse(listId)).thenReturn(Optional.of(fetchedEntity));
    when(listExportRepository.save(exportDetailsArgumentCaptor.capture())).thenReturn(exportDetails);

    when(listExportMapper.toListExportDTO(any(ExportDetails.class)))
      .thenReturn(mock(org.folio.list.domain.dto.ListExportDTO.class));
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(listExportWorkerService.doAsyncExport(exportDetails, userId)).thenReturn(CompletableFuture.completedFuture(true));
    when(entityTypeClient.getEntityType(fetchedEntity.getEntityTypeId(), ListActions.EXPORT)).thenReturn(entityType);
    doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(1);
      runnable.run();
      return CompletableFuture.completedFuture(null);
    })
      .when(systemUserScopedExecutionService)
      .executeAsyncSystemUserScoped(any(), any());

    listExportService.createExport(listId, fields);

    // Two calls made to listExportRepository.save()
    // 1. For saving the "IN_PROGRESS" export
    // 2. For saving the "SUCCESS" export
    ExportDetails inProgressExport = exportDetailsArgumentCaptor.getAllValues().get(0);
    ExportDetails successExport = exportDetailsArgumentCaptor.getAllValues().get(1);

    assertThat(userId).isEqualTo(inProgressExport.getCreatedBy());
    assertThat(listId).isEqualTo(inProgressExport.getList().getId());
    assertThat(inProgressExport.getStartDate()).isNotNull();
    assertThat(inProgressExport.getStatus()).hasToString(ListExportDTO.StatusEnum.IN_PROGRESS.toString());
    assertTrue(inProgressExport.getFields().containsAll(expectedExportFields));

    assertThat(exportDetails.getCreatedBy()).isEqualTo(successExport.getCreatedBy());
    assertThat(exportDetails.getList().getId()).isEqualTo(successExport.getList().getId());
    assertThat(exportDetails.getStartDate()).isEqualTo(successExport.getStartDate());
    assertThat(successExport.getEndDate()).isNotNull();
    assertThat(successExport.getStatus()).hasToString(ListExportDTO.StatusEnum.SUCCESS.toString());
  }


  @Test
  void shouldSaveFailedExportIfRefreshFail() {
    UUID listId = TestDataFixture.getListExportDetails().getList().getId();
    UUID userId = UUID.randomUUID();
    ListEntity fetchedEntity = TestDataFixture.getListExportDetails().getList();
    ExportDetails exportDetails = TestDataFixture.getListExportDetails();
    ArgumentCaptor<ExportDetails> exportDetailsArgumentCaptor = ArgumentCaptor.forClass(ExportDetails.class);
    when(listRepository.findByIdAndIsDeletedFalse(listId)).thenReturn(Optional.of(fetchedEntity));
    when(listExportRepository.save(exportDetailsArgumentCaptor.capture())).thenReturn(exportDetails);
    when(entityTypeClient.getEntityType(fetchedEntity.getEntityTypeId(), ListActions.EXPORT)).thenReturn(new EntityType());

    when(listExportMapper.toListExportDTO(any(ExportDetails.class)))
      .thenReturn(mock(org.folio.list.domain.dto.ListExportDTO.class));
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(listExportWorkerService.doAsyncExport(exportDetails, userId))
      .thenReturn(CompletableFuture.failedFuture(new RuntimeException("something went wrong")));
    doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(1);
      runnable.run();
      return CompletableFuture.completedFuture(null);
    })
      .when(systemUserScopedExecutionService)
      .executeAsyncSystemUserScoped(any(), any());

    listExportService.createExport(listId, null);

    // Two calls made to listExportRepository.save()
    // 1. For saving the "IN_PROGRESS" export
    // 2. For saving the "FAILED" export
    ExportDetails failedExport = exportDetailsArgumentCaptor.getAllValues().get(1);

    assertThat(exportDetails.getCreatedBy()).isEqualTo(failedExport.getCreatedBy());
    assertThat(exportDetails.getList().getId()).isEqualTo(failedExport.getList().getId());
    assertThat(exportDetails.getStartDate()).isEqualTo(failedExport.getStartDate());
    assertThat(failedExport.getEndDate()).isNotNull();
    assertThat(failedExport.getStatus()).hasToString(ListExportDTO.StatusEnum.FAILED.toString());
  }

  @Test
  void shouldReturnExportDetails() {
    UUID listId = TestDataFixture.getListExportDetails().getList().getId();
    UUID exportId = TestDataFixture.getListExportDetails().getExportId();
    ExportDetails exportDetails = TestDataFixture.getListExportDetails();
    ListExportDTO expectedlistExportDTO = TestDataFixture.getListExportDTO();
    when(listExportRepository.findByListIdAndExportId(listId, exportId)).thenReturn(Optional.of(exportDetails));
    when(listExportMapper.toListExportDTO(any(ExportDetails.class))).thenReturn(expectedlistExportDTO);

    var actualListExportDTO = listExportService.getExportDetails(listId, exportId);

    assertThat(expectedlistExportDTO).isEqualTo(actualListExportDTO);
  }

  @Test
  void shouldReturnExportNotFoundException() {
    UUID listId = TestDataFixture.getListExportDetails().getList().getId();
    UUID exportId = UUID.randomUUID();
    when(listExportRepository.findByListIdAndExportId(listId, exportId)).thenReturn(Optional.empty());
    assertThrows(ExportNotFoundException.class, () -> listExportService.getExportDetails(listId, exportId));
  }

  @Test
  void getListShouldThrowExceptionWhenValidationFailed() {
    UUID listId = UUID.randomUUID();
    ListEntity listEntity = new ListEntity();
    listEntity.setId(listId);
    UUID exportId = UUID.randomUUID();
    ExportDetails exportDetails = TestDataFixture.getListExportDetails();
    when(listExportRepository.findByListIdAndExportId(listId, exportId)).thenReturn(Optional.of(exportDetails));
    doThrow(new PrivateListOfAnotherUserException(listEntity, ListActions.EXPORT))
      .when(validationService)
      .validateExport(exportDetails.getList());
    Assertions.assertThrows(
      PrivateListOfAnotherUserException.class,
      () -> listExportService.getExportDetails(listId, exportId)
    );
  }

  @Test
  void downloadExportTest() {
    UUID listId = UUID.randomUUID();
    UUID exportId = UUID.randomUUID();
    String csvData = "xyz, item, 25, patron";
    InputStream csvInputStream = new ByteArrayInputStream(csvData.getBytes());
    String fileName = "Missing Items";
    long length = 1234;
    ExportDownloadContents expected = new ExportDownloadContents(fileName, csvInputStream, length);
    ExportDetails exportDetails = TestDataFixture.getListExportDetails();
    exportDetails.setExportId(exportId);

    doNothing().when(validationService).validateExport(exportDetails.getList());
    when(listExportRepository.findByListIdAndExportId(listId, exportId)).thenReturn(Optional.of(exportDetails));
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(folioS3Client.read(ExportUtils.getFileName(TENANT_ID, exportId))).thenReturn(csvInputStream);
    when(folioS3Client.getSize(ExportUtils.getFileName(TENANT_ID, exportId))).thenReturn(length);

    ExportDownloadContents actual = listExportService.downloadExport(listId, exportId);
    assertEquals(expected, actual);
  }

  @Test
  void downloadExportShouldReturnExportNotFound() {
    UUID listId = UUID.randomUUID();
    UUID exportId = UUID.randomUUID();
    when(listExportRepository.findByListIdAndExportId(listId, exportId)).thenReturn(Optional.empty());
    assertThrows(ExportNotFoundException.class, () -> listExportService.downloadExport(listId, exportId));
  }

  @Test
  void downloadShouldThrowExceptionWhenValidationFailed() {
    UUID listId = UUID.randomUUID();
    ListEntity listEntity = new ListEntity();
    listEntity.setId(listId);
    UUID exportId = UUID.randomUUID();
    ExportDetails exportDetails = TestDataFixture.getListExportDetails();
    when(listExportRepository.findByListIdAndExportId(listId, exportId)).thenReturn(Optional.of(exportDetails));
    doThrow(new PrivateListOfAnotherUserException(listEntity, ListActions.EXPORT))
      .when(validationService)
      .validateExport(exportDetails.getList());
    Assertions.assertThrows(
      PrivateListOfAnotherUserException.class,
      () -> listExportService.downloadExport(listId, exportId)
    );
  }

  @Test
  void shouldCancelExport() {
    ExportDetails exportDetails = TestDataFixture.getListExportDetails();
    ExportDetails cancelledExport = TestDataFixture.getListExportDetails();
    cancelledExport.setStatus(AsyncProcessStatus.CANCELLED);
    UUID listId = exportDetails.getList().getId();
    UUID exportId = exportDetails.getExportId();

    when(listExportRepository.findByListIdAndExportId(listId, exportId)).thenReturn(Optional.of(exportDetails));

    listExportService.cancelExport(listId, exportId);
    verify(listExportRepository, times(1)).save(cancelledExport);
  }

  @Test
  void cancelExportShouldThrowExceptionWhenExportNotFound() {
    UUID listId = UUID.randomUUID();
    UUID exportId = UUID.randomUUID();
    when(listExportRepository.findByListIdAndExportId(listId, exportId)).thenReturn(Optional.empty());
    assertThrows(ExportNotFoundException.class, () -> listExportService.cancelExport(listId, exportId));
  }

  @Test
  void shouldRegisterShutdownTask() {
    ExportDetails exportDetails = TestDataFixture.getListExportDetails();
    ListEntity fetchedEntity = exportDetails.getList();
    UUID listId = fetchedEntity.getId();
    when(listRepository.findByIdAndIsDeletedFalse(listId)).thenReturn(Optional.of(fetchedEntity));
    when(listExportRepository.save(any(ExportDetails.class))).thenReturn(exportDetails);
    when(listExportMapper.toListExportDTO(exportDetails)).thenReturn(mock(ListExportDTO.class));
    when(entityTypeClient.getEntityType(fetchedEntity.getEntityTypeId(), ListActions.EXPORT)).thenReturn(new EntityType());

    listExportService.createExport(listId, null);

    verify(appShutdownService, times(1))
      .registerShutdownTask(eq(folioExecutionContext), any(Runnable.class), any(String.class));
  }
}
