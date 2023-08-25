package org.folio.list.service;

import org.folio.list.domain.ListEntity;
import org.folio.list.exception.ListNotRefreshingException;
import org.folio.list.exception.PrivateListOfAnotherUserException;
import org.folio.list.services.ListValidationService;
import org.folio.list.utils.TestDataFixture;
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
class ListServiceValidateCancelRefreshTest {
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @InjectMocks
  private ListValidationService validationService;

  @Test
  void shouldValidateCancelRefresh() {
    ListEntity list = TestDataFixture.getListEntityWithInProgressRefresh();
    UUID userId = list.getCreatedBy();
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    assertDoesNotThrow(() -> validationService.validateCancelRefresh(list));
  }

  @Test
  void shouldThrowExceptionForPrivateListOwnedByOtherUser() {
    ListEntity list = TestDataFixture.getListEntityWithInProgressRefresh();
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    assertThrows(PrivateListOfAnotherUserException.class, () -> validationService.validateCancelRefresh(list));
}

  @Test
  void shouldThrowExceptionForNonRefreshingList() {
    ListEntity list = TestDataFixture.getListEntityWithSuccessRefresh();
    UUID userId = list.getCreatedBy();
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    assertThrows(ListNotRefreshingException.class, () -> validationService.validateCancelRefresh(list));
  }
}
