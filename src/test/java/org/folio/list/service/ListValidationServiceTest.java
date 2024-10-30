package org.folio.list.service;

import org.folio.fql.service.FqlValidationService;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.dto.ListRequestDTO;
import org.folio.list.domain.dto.ListUpdateRequestDTO;
import org.folio.list.exception.CrossTenantListMustBePrivateException;
import org.folio.list.exception.ListInactiveException;
import org.folio.list.exception.PrivateListOfAnotherUserException;
import org.folio.list.repository.ListExportRepository;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListValidationService;
import org.folio.list.utils.TestDataFixture;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListValidationServiceTest {

  @InjectMocks
  private ListValidationService listValidationService;

  @Mock
  private FolioExecutionContext folioExecutionContext;

  @Mock
  private FqlValidationService fqlValidationService;

  @Mock
  private EntityTypeClient entityTypeClient;

  @Mock
  private ListExportRepository listExportRepository;

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

  @Test
  void crossTenantListsCanOnlyBeCreatedPrivate() {
    ListRequestDTO createRequestPrivate = TestDataFixture.getListRequestDTO().isPrivate(true);
    ListRequestDTO createRequestShared = TestDataFixture.getListRequestDTO().isPrivate(false);

    EntityType entityType = new EntityType().crossTenantQueriesEnabled(true);

    when(fqlValidationService.validateFql(any(), any())).thenReturn(Map.of());

    assertDoesNotThrow(() -> listValidationService.validateCreate(createRequestPrivate, entityType));
    assertThrows(
      CrossTenantListMustBePrivateException.class,
      () -> listValidationService.validateCreate(createRequestShared, entityType)
    );
  }

  @Test
  void crossTenantListsCanOnlyBeUpdatedPrivate() {
    // the choice of original list does not really matter here, it's not considered for this test
    ListEntity originalList = TestDataFixture.getSharedNonCannedListEntity().withVersion(3);

    ListUpdateRequestDTO saveRequestPrivate = TestDataFixture.getListUpdateRequestDTO().isPrivate(true);
    ListUpdateRequestDTO saveRequestShared = TestDataFixture.getListUpdateRequestDTO().isPrivate(false);

    EntityType entityType = new EntityType().crossTenantQueriesEnabled(true);

    when(fqlValidationService.validateFql(any(), any())).thenReturn(Map.of());
    when(listExportRepository.isExporting(any())).thenReturn(false);

    assertDoesNotThrow(() -> listValidationService.validateUpdate(originalList, saveRequestPrivate, entityType));
    assertThrows(
      CrossTenantListMustBePrivateException.class,
      () -> listValidationService.validateUpdate(originalList, saveRequestShared, entityType)
    );
  }

  @Test
  void nonCrossTenantListsCanBeShared() {
    // the choice of original list does not really matter here, it's not considered for this test
    ListEntity originalList = TestDataFixture.getSharedNonCannedListEntity().withVersion(3);

    ListRequestDTO createRequestPrivate = TestDataFixture.getListRequestDTO().isPrivate(true);
    ListRequestDTO createRequestShared = TestDataFixture.getListRequestDTO().isPrivate(false);

    ListUpdateRequestDTO saveRequestPrivate = TestDataFixture.getListUpdateRequestDTO().isPrivate(true);
    ListUpdateRequestDTO saveRequestShared = TestDataFixture.getListUpdateRequestDTO().isPrivate(false);

    EntityType entityType = new EntityType().crossTenantQueriesEnabled(false);

    when(fqlValidationService.validateFql(any(), any())).thenReturn(Map.of());
    when(listExportRepository.isExporting(any())).thenReturn(false);

    assertDoesNotThrow(() -> listValidationService.validateCreate(createRequestPrivate, entityType));
    assertDoesNotThrow(() -> listValidationService.validateCreate(createRequestShared, entityType));
    assertDoesNotThrow(() -> listValidationService.validateUpdate(originalList, saveRequestPrivate, entityType));
    assertDoesNotThrow(() -> listValidationService.validateUpdate(originalList, saveRequestShared, entityType));
  }
}
