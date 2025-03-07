package org.folio.list.service;

import org.folio.fql.service.FqlValidationService;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.dto.ListUpdateRequestDTO;
import org.folio.list.exception.*;
import org.folio.list.repository.ListExportRepository;
import org.folio.list.services.ListValidationService;
import org.folio.list.util.TestDataFixture;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
class ListServiceValidateUpdateTest {

  @InjectMocks
  private ListValidationService listValidationService;
  @Mock
  private FqlValidationService fqlValidationService;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private ListExportRepository listExportRepository;

  @Test
  void shouldReturnErrorOnListVersionMismatch() {
    ListEntity entity = TestDataFixture.getSharedNonCannedListEntity();
    EntityType entityType = new EntityType().name("test-entity");
    ListUpdateRequestDTO updateRequestDto = TestDataFixture.getListUpdateRequestDTO();
    updateRequestDto.setVersion(entity.getVersion() - 1);
    assertThrows(OptimisticLockException.class,
      () -> listValidationService.validateUpdate(entity, updateRequestDto, entityType));
  }

  @Test
  void shouldReturnErrorWhenSharedListIsCanned() {
    ListEntity entity = TestDataFixture.getSharedNonCannedListEntity();
    EntityType entityType = new EntityType().name("test-entity");
    ListUpdateRequestDTO updateRequestDto = TestDataFixture.getListUpdateRequestDTO();
    entity.setIsCanned(true);
    assertThrows(ListIsCannedException.class,
      () -> listValidationService.validateUpdate(entity, updateRequestDto, entityType));
  }

  @Test
  void shouldReturnErrorWhenListIsRefreshing() {
    ListEntity entity = TestDataFixture.getListEntityWithInProgressRefresh();
    EntityType entityType = new EntityType().name("test-entity");
    ListUpdateRequestDTO updateRequestDto = TestDataFixture.getListUpdateRequestDTO();
    entity.setIsPrivate(false);
    entity.setIsCanned(false);
    assertThrows(RefreshInProgressException.class,
      () -> listValidationService.validateUpdate(entity, updateRequestDto, entityType));
  }

  @Test
  void shouldReturnErrorWhenListIsExporting() {
    ListEntity list = TestDataFixture.getListExportDetails().getList();
    EntityType entityType = new EntityType().name("test-entity");
    ListUpdateRequestDTO updateRequestDto = TestDataFixture.getListUpdateRequestDTO();
    list.setIsPrivate(false);
    list.setIsCanned(false);
    when(listExportRepository.isExporting(list.getId())).thenReturn(true);
    when(fqlValidationService.validateFql(entityType, updateRequestDto.getFqlQuery()))
      .thenReturn(Map.of());
    assertThrows(ExportInProgressException.class,
      () -> listValidationService.validateUpdate(list, updateRequestDto, entityType));
  }

  @Test
  void shouldReturnErrorWhenListIsPrivate() {
    ListEntity entity = TestDataFixture.getPrivateListEntity();
    EntityType entityType = new EntityType().name("test-entity");
    ListUpdateRequestDTO updateRequestDto = TestDataFixture.getListUpdateRequestDTO();
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    assertThrows(PrivateListOfAnotherUserException.class,
      () -> listValidationService.validateUpdate(entity, updateRequestDto, entityType));
  }

  @Test
  void shouldReturnErrorForInvalidFql() {
    ListEntity entity = TestDataFixture.getSharedNonCannedListEntity();
    EntityType entityType = new EntityType().name("test-entity");
    ListUpdateRequestDTO updateRequest = TestDataFixture.getListUpdateRequestDTO();
    updateRequest.setVersion(entity.getVersion());
    when(fqlValidationService.validateFql(entityType, updateRequest.getFqlQuery())).thenReturn(Map.of("field1", "Field is invalid"));
    assertThrows(InvalidFqlException.class, () -> listValidationService.validateUpdate(entity, updateRequest, entityType));
  }

  @Test
  void shouldNotValidateEmptyFql() {
    ListEntity entity = TestDataFixture.getSharedNonCannedListEntity();
    EntityType entityType = new EntityType().name("test-entity");
    ListUpdateRequestDTO updateRequest = TestDataFixture.getListUpdateRequestDTO();
    updateRequest.setVersion(entity.getVersion());

    updateRequest.setFqlQuery("");
    listValidationService.validateUpdate(entity, updateRequest, entityType);
    verify(fqlValidationService, never()).validateFql(any(), any());

    updateRequest.setFqlQuery(null);
    listValidationService.validateUpdate(entity, updateRequest, entityType);
    verify(fqlValidationService, never()).validateFql(any(), any());
  }

  @Test
  void shouldNotReturnErrorForValidRequests() {
    ListEntity entity = TestDataFixture.getSharedNonCannedListEntity();
    EntityType entityType = new EntityType().name("test-entity");
    ListUpdateRequestDTO updateRequest = TestDataFixture.getListUpdateRequestDTO();
    updateRequest.setVersion(entity.getVersion());
    when(fqlValidationService.validateFql(entityType, updateRequest.getFqlQuery())).thenReturn(Map.of());
    assertDoesNotThrow(() -> listValidationService.validateUpdate(entity, updateRequest, entityType));
  }
}
