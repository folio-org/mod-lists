package org.folio.list.service;

import org.folio.list.domain.ListEntity;
import org.folio.list.exception.PrivateListOfAnotherUserException;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListServiceValidateGetExportTest {
  @InjectMocks
  private ListValidationService validationService;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private EntityTypeClient entityTypeClient;

  @Test
  void getExportDetailsShouldReturnErrorWhenListIsPrivate() {
    UUID listId = UUID.randomUUID();
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh(listId);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    assertThrows(PrivateListOfAnotherUserException.class, () -> validationService.validateExport(entity));
  }
}
