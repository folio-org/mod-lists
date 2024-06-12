package org.folio.list.service;

import org.folio.list.domain.ListEntity;
import org.folio.list.exception.*;
import org.folio.list.repository.ListExportRepository;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.services.ListValidationService;
import org.folio.list.utils.TestDataFixture;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListServiceValidateRefreshTest {
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private ListExportRepository listExportRepository;
  @Mock
  private EntityTypeClient entityTypeClient;
  @InjectMocks
  private ListValidationService validationService;


  @Test
  void shouldNotPerformRefreshWhenListIsInactive() {
    ListEntity entity = TestDataFixture.getInactiveListEntity();
    when(folioExecutionContext.getUserId()).thenReturn(entity.getCreatedBy());
    assertThrows(ListInactiveException.class, () -> validationService.validateRefresh(entity));
  }

  @Test
  void shouldNotPerformRefreshWhenAnotherRefreshInProgress() {
    ListEntity entity = TestDataFixture.getListEntityWithInProgressRefresh();
    when(folioExecutionContext.getUserId()).thenReturn(entity.getCreatedBy());
    assertThrows(RefreshInProgressException.class, () -> validationService.validateRefresh(entity));
  }

  @Test
  void shouldNotPerformRefreshOfPrivateListOwnedByAnotherUser() {
    ListEntity entity = TestDataFixture.getPrivateListEntity();
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    assertThrows(PrivateListOfAnotherUserException.class, () -> validationService.validateRefresh(entity));
  }

  @Test
  void shouldNotThrowExceptionWhenListIsRefreshable() {
    ListEntity refreshableList = TestDataFixture.getSharedNonCannedListEntity();
    assertDoesNotThrow(() -> validationService.validateRefresh(refreshableList));
  }

  @Test
  void shouldReturnErrorWhenListIsExporting() {
    ListEntity list = TestDataFixture.getListExportDetails().getList();
    when(listExportRepository.isExporting(list.getId())).thenReturn(true);
    assertThrows(ExportInProgressException.class, () -> validationService.validateRefresh(list));
  }

  @Test
  void shouldNotPerformRefreshWhenListHasNoQuery() {
    ListEntity list = TestDataFixture.getListEntityWithoutQuery();
    // For the purposes of validation, "no query" could mean a query that is null or an empty string, so test both
    for (var query : Arrays.asList(null, "", " ", "\t", "\n")) { // Arrays.asList() because List.of() doesn't allow nulls
      list.setFqlQuery(query);
      assertThrows(MissingQueryException.class, () -> validationService.validateRefresh(list));
    }
  }
}
