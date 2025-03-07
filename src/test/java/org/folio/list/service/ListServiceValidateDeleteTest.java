package org.folio.list.service;

import org.folio.list.domain.ListEntity;
import org.folio.list.exception.ListIsCannedException;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListServiceValidateDeleteTest {

  @InjectMocks
  private ListValidationService listValidationService;

  @Mock
  private FolioExecutionContext folioExecutionContext;

  @Mock
  private ListExportRepository listExportRepository;

  @Mock
  private EntityTypeClient entityTypeClient;

  @Test
  void shouldReturnErrorWhenSharedListIsCanned() {
    ListEntity entity = TestDataFixture.getSharedNonCannedListEntity();
    entity.setIsCanned(true);
    assertThrows(ListIsCannedException.class, () -> listValidationService.validateDelete(entity));
  }

  @Test
  void shouldReturnErrorWhenListIsRefreshing() {
    ListEntity entity = TestDataFixture.getListEntityWithInProgressRefresh();
    entity.setIsPrivate(false);
    entity.setIsCanned(false);
    assertThrows(RefreshInProgressException.class, () -> listValidationService.validateDelete(entity));
  }

  @Test
  void shouldReturnErrorWhenListIsPrivate() {
    ListEntity entity = TestDataFixture.getPrivateListEntity();
    assertThrows(PrivateListOfAnotherUserException.class, () -> listValidationService.validateDelete(entity));
  }

  @Test
  void shouldNotReturnErrorForDeletableList_Shared() {
    ListEntity entity = TestDataFixture.getSharedNonCannedListEntity();
    assertDoesNotThrow(() -> listValidationService.validateDelete(entity));
  }

  @Test
  void shouldNotReturnErrorForDeletableList_OwnedByUser() {
    ListEntity entity = TestDataFixture.getPrivateListEntity();
    when(folioExecutionContext.getUserId()).thenReturn(entity.getCreatedBy());
    assertDoesNotThrow(() -> listValidationService.validateDelete(entity));
  }

  @Test
  void shouldReturnErrorWhenListIsExporting() {
    ListEntity list = TestDataFixture.getListExportDetails().getList();
    when(listExportRepository.isExporting(list.getId())).thenReturn(true);
    assertThrows(ExportInProgressException.class, () -> listValidationService.validateRefresh(list));
  }
}
