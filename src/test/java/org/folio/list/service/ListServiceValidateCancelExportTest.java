package org.folio.list.service;

import org.folio.list.domain.AsyncProcessStatus;
import org.folio.list.domain.ExportDetails;
import org.folio.list.domain.ListEntity;
import org.folio.list.exception.ExportNotFoundException;
import org.folio.list.exception.PrivateListOfAnotherUserException;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.services.ListActions;
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
class ListServiceValidateCancelExportTest {
  @InjectMocks
  private ListValidationService validationService;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private EntityTypeClient entityTypeClient;

  @Test
  void shouldValidateCancelExport() {
    ExportDetails exportDetails = TestDataFixture.getListExportDetails();
    ListEntity list = exportDetails.getList();
    list.setIsPrivate(true);
    UUID userId = list.getCreatedBy();
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(entityTypeClient.getEntityType(list.getEntityTypeId(), ListActions.CANCEL_EXPORT)).thenReturn(TestDataFixture.TEST_ENTITY_TYPE);
    assertDoesNotThrow(() -> validationService.validateCancelExport(exportDetails));
  }

  @Test
  void shouldThrowExceptionForPrivateListOwnerByOtherUser() {
    ExportDetails exportDetails = TestDataFixture.getListExportDetails();
    ListEntity list = exportDetails.getList();
    list.setIsPrivate(true);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(entityTypeClient.getEntityType(list.getEntityTypeId(), ListActions.CANCEL_EXPORT)).thenReturn(TestDataFixture.TEST_ENTITY_TYPE);
    assertThrows(PrivateListOfAnotherUserException.class, () -> validationService.validateCancelExport(exportDetails));
  }

  @Test
  void shouldThrowExceptionForNonExportingList() {
    ExportDetails exportDetails = TestDataFixture.getListExportDetails();
    exportDetails.setStatus(AsyncProcessStatus.SUCCESS);
    assertThrows(ExportNotFoundException.class, () -> validationService.validateCancelExport(exportDetails));
  }
}
