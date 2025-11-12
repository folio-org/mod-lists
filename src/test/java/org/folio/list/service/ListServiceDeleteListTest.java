package org.folio.list.service;

import org.folio.list.domain.ListEntity;
import org.folio.list.exception.*;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListRepository;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListService;
import org.folio.list.services.ListValidationService;
import org.folio.list.util.TestDataFixture;
import org.folio.querytool.domain.dto.UpdateUsedByRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListServiceDeleteListTest {

  @InjectMocks
  private ListService listService;

  @Mock
  private ListRepository listRepository;

  @Mock
  private ListValidationService listValidationService;

  @Mock
  private ListContentsRepository listContentsRepository;

  @Mock
  private EntityTypeClient entityTypeClient;

  @Test
  void shouldDeleteList() {
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh();
    UpdateUsedByRequest updateUsedByRequest = new UpdateUsedByRequest()
      .name("mod-lists")
      .operation(UpdateUsedByRequest.OperationEnum.REMOVE);

    when(listRepository.findByIdAndIsDeletedFalse(entity.getId())).thenReturn(Optional.of(entity));
    when(listRepository.searchList(null, null, List.of(entity.getEntityTypeId()), null, null, null, false, null))
      .thenReturn(Page.empty());

    listValidationService.validateDelete(entity);
    listService.deleteList(entity.getId());

    verify(listContentsRepository, times(1)).deleteContents(entity.getId());
    verify(entityTypeClient, times(1)).updateEntityTypeUsedBy(
      entity.getEntityTypeId(),
      updateUsedByRequest
    );

    // soft delete saves the entity with is_deleted=true
    ArgumentCaptor<ListEntity> captor = ArgumentCaptor.forClass(ListEntity.class);
    verify(listRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue().getIsDeleted()).isTrue();
  }

  @Test
  void shouldNotSendUpdateUsedByRequestIfListsStillUseEntityType() {
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh();

    when(listRepository.findByIdAndIsDeletedFalse(entity.getId())).thenReturn(Optional.of(entity));
    when(listRepository.searchList(null, null, List.of(entity.getEntityTypeId()), null, null, null, false, null))
      .thenReturn(new PageImpl<>(List.of(entity)));

    listService.deleteList(entity.getId());

    verify(entityTypeClient, times(0)).updateEntityTypeUsedBy(
      any(),
      any()
    );
  }

  @Test
  void shouldNotDeleteIfValidationFailed() {
    ListEntity entity = TestDataFixture.getPrivateListEntity();
    when(listRepository.findByIdAndIsDeletedFalse(entity.getId())).thenReturn(Optional.of(entity));
    doThrow(new PrivateListOfAnotherUserException(entity, ListActions.DELETE))
      .when(listValidationService).validateDelete(entity);
    UUID entityId = entity.getId();
    assertThrows(PrivateListOfAnotherUserException.class, () -> listService.deleteList(entityId));
  }

  @Test
  void shouldThrowError404WhenListNotFound() {
    UUID listId = UUID.randomUUID();
    assertThrows(ListNotFoundException.class, () -> listService.deleteList(listId));
  }
}
