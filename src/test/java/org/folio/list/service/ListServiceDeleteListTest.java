package org.folio.list.service;

import org.folio.list.domain.ListEntity;
import org.folio.list.exception.*;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListRepository;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListService;
import org.folio.list.services.ListValidationService;
import org.folio.list.utils.TestDataFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
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

  @Test
  void shouldDeleteList() {
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh();
    when(listRepository.findByIdAndIsDeletedFalse(entity.getId())).thenReturn(Optional.of(entity));
    
    listValidationService.validateDelete(entity);
    listService.deleteList(entity.getId());

    verify(listContentsRepository, times(1)).deleteContents(entity.getId());

    // soft delete saves the entity with is_deleted=true
    ArgumentCaptor<ListEntity> captor = ArgumentCaptor.forClass(ListEntity.class);
    verify(listRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue().getIsDeleted()).isTrue();
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
