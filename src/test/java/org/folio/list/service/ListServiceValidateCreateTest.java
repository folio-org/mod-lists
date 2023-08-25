package org.folio.list.service;

import org.folio.fqm.lib.service.FqlValidationService;
import org.folio.list.domain.dto.ListRequestDTO;
import org.folio.list.exception.InvalidFqlException;
import org.folio.list.services.ListValidationService;
import org.folio.list.utils.TestDataFixture;
import org.folio.querytool.domain.dto.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListServiceValidateCreateTest {

  @InjectMocks
  private ListValidationService listValidationService;
  @Mock
  private FqlValidationService fqlValidationService;

  @Test
  void shouldReturnErrorForInvalidFql() {
    EntityType entityType = new EntityType().name("test-entity");
    ListRequestDTO createRequest = TestDataFixture.getListRequestDTO();
    when(fqlValidationService.validateFql(entityType, createRequest.getFqlQuery()))
      .thenReturn(Map.of("field1", "field is invalid"));
    assertThrows(InvalidFqlException.class, () -> listValidationService.validateCreate(createRequest, entityType));
  }

  @Test
  void shouldNotValidateEmptyFql() {
    ListRequestDTO createRequest = TestDataFixture.getListRequestDTO();
    EntityType entityType = new EntityType().name("test-entity");
    createRequest.setFqlQuery("");
    listValidationService.validateCreate(createRequest, entityType);
    verify(fqlValidationService, never()).validateFql(any(), any());

    createRequest.setFqlQuery(null);
    listValidationService.validateCreate(createRequest, entityType);
    verify(fqlValidationService, never()).validateFql(any(), any());
  }

  @Test
  void shouldNotReturnErrorForValidRequest() {
    EntityType entityType = new EntityType().name("test-entity");
    ListRequestDTO createRequest = TestDataFixture.getListRequestDTO();
    when(fqlValidationService.validateFql(entityType, createRequest.getFqlQuery())).thenReturn(Map.of());
    assertDoesNotThrow(() -> listValidationService.validateCreate(createRequest, entityType));
  }
}
