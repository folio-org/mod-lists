package org.folio.list.service;

import org.folio.list.domain.ListEntity;
import org.folio.list.exception.ExportInProgressException;
import org.folio.list.exception.PrivateListOfAnotherUserException;
import org.folio.list.exception.RefreshInProgressException;
import org.folio.list.repository.ListExportRepository;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.services.ListValidationService;
import org.folio.list.util.TestDataFixture;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListServiceValidateCreateExportTest {
  @InjectMocks
  private ListValidationService validationService;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private ListExportRepository listExportRepository;
  @Mock
  private EntityTypeClient entityTypeClient;

  @Test
  void shouldReturnErrorWhenListIsPrivate() {
    UUID listId = UUID.randomUUID();
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh(listId);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    assertThrows(PrivateListOfAnotherUserException.class, () -> validationService.validateCreateExport(entity));
  }

  @Test
  void shouldReturnErrorWhenListIsRefreshing() {
    ListEntity entity = TestDataFixture.getListEntityWithInProgressRefresh();
    entity.setIsPrivate(false);
    assertThrows(RefreshInProgressException.class, () -> validationService.validateCreateExport(entity));
  }

  @Test
  void shouldNotThrowExceptionWhenListIsExportable() {
    UUID listId = UUID.randomUUID();
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh(listId);
    when(folioExecutionContext.getUserId()).thenReturn(entity.getCreatedBy());
    when(listExportRepository.isUserAlreadyExporting(listId, entity.getCreatedBy())).thenReturn(false);
    assertDoesNotThrow(() -> validationService.validateCreateExport(entity));
  }

  @Test
  void shouldReturnErrorWhenExportInProgressForSameListBySameUser() {
    ListEntity entity = TestDataFixture.getListExportDetails().getList();
    when(folioExecutionContext.getUserId()).thenReturn(entity.getCreatedBy());
    when(listExportRepository.isUserAlreadyExporting(entity.getId(), entity.getCreatedBy())).thenReturn(true);
    assertThrows(ExportInProgressException.class, () -> validationService.validateCreateExport(entity));
  }
}
