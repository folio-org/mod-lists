package org.folio.list.service;

import feign.FeignException;
import org.folio.list.domain.ListEntity;
import org.folio.list.exception.InsufficientEntityTypePermissionsException;
import org.folio.list.exception.PrivateListOfAnotherUserException;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.services.ListValidationService;
import org.folio.list.utils.TestDataFixture;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListServiceValidateReadTest {
  @InjectMocks
  private ListValidationService validationService;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private EntityTypeClient entityTypeClient;

  @Test
  void shouldValidateRead() {
    ListEntity entity = TestDataFixture.getListEntityWithoutQuery();
    assertDoesNotThrow(() -> validationService.validateRead(entity));
  }

  @Test
  void validateReadShouldThrowErrorForUserMissingPermissions() {
    ListEntity entity = TestDataFixture.getListEntityWithoutQuery();
    when(entityTypeClient.getEntityType(entity.getEntityTypeId()))
      .thenThrow(new FeignException.Unauthorized("[{\"User is missing permissions: [foo.bar]\"}]", mock(feign.Request.class), null, null));
    assertThrows(InsufficientEntityTypePermissionsException.class, () -> validationService.validateRead(entity));
  }

  @Test
  void validateReadShouldThrowErrorForMissingEntityType() {
    ListEntity entity = TestDataFixture.getListEntityWithoutQuery();
    when(entityTypeClient.getEntityType(entity.getEntityTypeId()))
      .thenThrow(new FeignException.NotFound("Entity type not found", mock(feign.Request.class), null, null));
    assertThrows(NotFoundException.class, () -> validationService.validateRead(entity));
  }

  @Test
  void validateReadShouldThrowErrorForPrivateListOfAnotherUser() {
    UUID listId = UUID.randomUUID();
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh(listId);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    assertThrows(PrivateListOfAnotherUserException.class, () -> validationService.validateRead(entity));
  }
}
