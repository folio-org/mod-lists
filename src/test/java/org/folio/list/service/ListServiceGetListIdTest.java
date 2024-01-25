package org.folio.list.service;


import org.folio.list.domain.ListEntity;
import org.folio.list.exception.PrivateListOfAnotherUserException;
import org.folio.list.mapper.ListMapper;
import org.folio.list.repository.ListRepository;
import org.folio.list.domain.dto.ListDTO;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListService;
import org.folio.list.services.ListValidationService;
import org.folio.list.utils.TestDataFixture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListServiceGetListIdTest {

  @InjectMocks
  private ListService listService;

  @Mock
  private ListRepository listRepository;

  @Mock
  private ListMapper listMapper;

  @Mock
  private ListValidationService listValidationService;

  @Test
  void testGetListById() {
    UUID listId = UUID.randomUUID();
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh(UUID.randomUUID());
    ListDTO listDto = TestDataFixture.getListDTOSuccessRefresh(listId);

    when(listRepository.findByIdAndIsDeletedFalse(listId)).thenReturn(Optional.of(entity));
    when(listMapper.toListDTO(entity)).thenReturn(listDto);
    var actual = listService.getListById(listId);
    assertThat(actual).contains(listDto);
  }

  @Test
  void shouldThrowExceptionWhenValidationFailed() {
    UUID listId = UUID.randomUUID();
    ListEntity listEntity = TestDataFixture.getNeverRefreshedListEntity();
    when(listRepository.findByIdAndIsDeletedFalse(listId)).thenReturn(Optional.of(listEntity));
    doThrow(new PrivateListOfAnotherUserException(listEntity, ListActions.READ))
      .when(listValidationService).assertSharedOrOwnedByUser(listEntity, ListActions.READ);
    Assertions.assertThrows(PrivateListOfAnotherUserException.class, () -> listService.getListById(listId));
  }
}
