package org.folio.list.service;

import jakarta.persistence.EntityManager;
import org.folio.list.domain.AsyncProcessStatus;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.ListRefreshDetails;
import org.folio.list.exception.PrivateListOfAnotherUserException;
import org.folio.list.mapper.ListRefreshMapper;
import org.folio.list.repository.ListRepository;
import org.folio.list.rest.UsersClient;
import org.folio.list.services.AppShutdownService;
import org.folio.list.services.EntityManagerFlushService;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListService;
import org.folio.list.services.ListValidationService;
import org.folio.list.services.refresh.ListRefreshService;
import org.folio.list.util.TaskTimer;
import org.folio.list.utils.TestDataFixture;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListServicePostRefreshTest {

  @InjectMocks
  private ListService listService;

  @Mock
  private ListRefreshService listRefreshService;

  @Mock
  private ListRepository listRepository;

  @Mock
  private ListRefreshMapper refreshMapper;

  @Mock
  private FolioExecutionContext executionContext;

  @Mock
  private UsersClient usersClient;

  @Mock
  private ListValidationService listValidationService;

  @Mock
  private AppShutdownService appShutdownService;

  private final EntityManagerFlushService entityManagerFlushService = spy(new EntityManagerFlushService(mock(EntityManager.class)));

  @Test
  void shouldPerformRefresh() {
    UUID userId = UUID.randomUUID();
    UsersClient.User user = new UsersClient.User(userId, Optional.of(new UsersClient.Personal("firstname", "lastname")));
    when(usersClient.getUser(userId)).thenReturn(user);
    ListEntity fetchedEntity = TestDataFixture.getListEntityWithSuccessRefresh();
    ListEntity savedEntity = TestDataFixture.getListEntityWithSuccessRefresh();
    org.folio.list.domain.dto.ListRefreshDTO inProgressRefreshDTO = TestDataFixture.getListRefreshDTO();
    ListRefreshDetails inProgressRefreshEntity = ListRefreshDetails.builder().build();

    savedEntity.setInProgressRefresh(inProgressRefreshEntity);

    when(listRepository.findById(savedEntity.getId())).thenReturn(Optional.of(fetchedEntity));
    when(listRepository.save(fetchedEntity)).thenReturn(savedEntity);
    when(refreshMapper.toListRefreshDTO(inProgressRefreshEntity)).thenReturn(inProgressRefreshDTO);
    when(executionContext.getUserId()).thenReturn(userId);

    Optional<org.folio.list.domain.dto.ListRefreshDTO> refreshDetails = listService.performRefresh(savedEntity.getId());
    verify(listRefreshService, times(1)).doAsyncRefresh(eq(savedEntity), isNull(), any(TaskTimer.class));
    verify(entityManagerFlushService, atLeastOnce()).flush();
    assertThat(refreshDetails).contains(inProgressRefreshDTO);
    verify(appShutdownService, times(1)).registerShutdownTask(eq(executionContext), any(Runnable.class), any(String.class));
  }

  @Test
  void shouldSaveInProgressRefreshDetails() {
    UUID listId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UsersClient.User user = new UsersClient.User(userId, Optional.of(new UsersClient.Personal("firstname", "lastname")));
    String username = "lastname, firstname";
    ListEntity fetchedEntity = TestDataFixture.getListEntityWithSuccessRefresh();

    ArgumentCaptor<ListEntity> listEntityCaptor = ArgumentCaptor.forClass(ListEntity.class);
    when(listRepository.findById(listId)).thenReturn(Optional.of(fetchedEntity));
    when(listRepository.save(listEntityCaptor.capture())).thenReturn(fetchedEntity);
    when(refreshMapper.toListRefreshDTO(any(ListRefreshDetails.class))).thenReturn(mock(org.folio.list.domain.dto.ListRefreshDTO.class));
    when(executionContext.getUserId()).thenReturn(userId);
    when(usersClient.getUser(userId)).thenReturn(user);

    listService.performRefresh(listId);
    ListEntity savedEntity = listEntityCaptor.getValue();
    ListRefreshDetails inProgressRefresh = savedEntity.getInProgressRefresh();
    assertThat(inProgressRefresh.getStatus()).isEqualTo(AsyncProcessStatus.IN_PROGRESS);
    assertThat(inProgressRefresh.getRefreshedBy()).isEqualTo(userId);
    assertThat(inProgressRefresh.getRefreshedByUsername()).isEqualTo(username);
  }

  @Test
  void shouldSaveInProgressRefreshForInvalidUserId() {
    UUID listId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    ListEntity fetchedEntity = TestDataFixture.getListEntityWithSuccessRefresh();

    ArgumentCaptor<ListEntity> listEntityCaptor = ArgumentCaptor.forClass(ListEntity.class);
    when(listRepository.findById(listId)).thenReturn(Optional.of(fetchedEntity));
    when(listRepository.save(listEntityCaptor.capture())).thenReturn(fetchedEntity);
    when(refreshMapper.toListRefreshDTO(any(ListRefreshDetails.class))).thenReturn(mock(org.folio.list.domain.dto.ListRefreshDTO.class));
    when(executionContext.getUserId()).thenReturn(userId);
    when(usersClient.getUser(userId)).thenThrow(NotFoundException.class);

    listService.performRefresh(listId);
    ListEntity savedEntity = listEntityCaptor.getValue();
    ListRefreshDetails inProgressRefresh = savedEntity.getInProgressRefresh();
    assertThat(inProgressRefresh.getStatus()).isEqualTo(AsyncProcessStatus.IN_PROGRESS);
    assertThat(inProgressRefresh.getRefreshedBy()).isEqualTo(userId);
    assertThat(inProgressRefresh.getRefreshedByUsername()).isEqualTo(userId.toString());
  }

  @Test
  void shouldThrowExceptionWhenValidationFailed() {
    UUID listId = UUID.randomUUID();
    ListEntity listEntity = TestDataFixture.getNeverRefreshedListEntity();
    when(listRepository.findById(listId)).thenReturn(Optional.of(listEntity));
    doThrow(new PrivateListOfAnotherUserException(listEntity, ListActions.REFRESH))
      .when(listValidationService).validateRefresh(listEntity);
    Assertions.assertThrows(PrivateListOfAnotherUserException.class, () -> listService.performRefresh(listId));
  }
}

