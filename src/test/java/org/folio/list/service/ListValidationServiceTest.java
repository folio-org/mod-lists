package org.folio.list.service;

import org.folio.list.domain.ListEntity;
import org.folio.list.exception.ListInactiveException;
import org.folio.list.exception.PrivateListOfAnotherUserException;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListValidationService;
import org.folio.list.utils.TestDataFixture;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListValidationServiceTest {

  @InjectMocks
  private ListValidationService listValidationService;

  @Mock
  private FolioExecutionContext folioExecutionContext;

  @Mock
  private EntityTypeClient entityTypeClient;


  @Test
  void shouldValidatePrivateListOwnedByUser() {
    ListEntity listEntity = TestDataFixture.getPrivateListEntity();
    when(folioExecutionContext.getUserId()).thenReturn(listEntity.getCreatedBy());
    assertDoesNotThrow(() -> listValidationService.assertSharedOrOwnedByUser(listEntity, ListActions.READ));
  }

  @Test
  void shouldReturnErrorForPrivateListOwnedByDifferentUser() {
    ListEntity listEntity = TestDataFixture.getPrivateListEntity();
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    assertThrows(PrivateListOfAnotherUserException.class,
      () -> listValidationService.assertSharedOrOwnedByUser(listEntity, ListActions.READ));
  }

  @Test
  void shouldValidateSharedList() {
    ListEntity listEntity = TestDataFixture.getPrivateListEntity();
    listEntity.setIsPrivate(false);
    assertDoesNotThrow(() -> listValidationService.assertSharedOrOwnedByUser(listEntity, ListActions.READ));
  }

  @Test
  void inactiveListsShouldNotBeExportable() {
    ListEntity inactiveList = TestDataFixture.getInactiveListEntity();
    assertFalse(inactiveList.getIsActive(), "The list should be inactive");

    assertThrows(ListInactiveException.class, () -> listValidationService.validateCreateExport(inactiveList));
  }
}
