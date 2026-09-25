package org.folio.list.service;

import org.folio.list.domain.ListEntity;
import org.folio.list.domain.dto.RelatedUserCollection;
import org.folio.list.repository.ListRepository;
import org.folio.list.rest.UsersClient;
import org.folio.list.rest.UsersClient.Personal;
import org.folio.list.rest.UsersClient.User;
import org.folio.list.services.UserService;
import org.folio.list.util.TestDataFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @InjectMocks
  private UserService userService;

  @Mock
  private ListRepository listRepository;

  @Mock
  private UsersClient usersClient;

  @Test
  void getRelatedUsersByRole_create_returnsCreatedByUsers() {
    UUID userId = UUID.randomUUID();
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh();
    entity.setCreatedBy(userId);

    User user = new User(userId, Optional.of(new Personal("John", "Doe")));
    when(listRepository.findAll()).thenReturn(List.of(entity));
    when(usersClient.getUser(userId)).thenReturn(user);

    RelatedUserCollection result = userService.getRelatedUsersByRole("create");

    assertThat(result.getTotalRecords()).isEqualTo(1);
    assertThat(result.getRelatedUsers()).hasSize(1);
    assertThat(result.getRelatedUsers().get(0).getId()).isEqualTo(userId.toString());
    assertThat(result.getRelatedUsers().get(0).getFullName()).isEqualTo("Doe, John");
  }

  @Test
  void getRelatedUsersByRole_update_returnsUpdatedByUsers() {
    UUID userId = UUID.randomUUID();
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh();
    entity.setUpdatedBy(userId);

    User user = new User(userId, Optional.of(new Personal("Jane", "Smith")));
    when(listRepository.findAll()).thenReturn(List.of(entity));
    when(usersClient.getUser(userId)).thenReturn(user);

    RelatedUserCollection result = userService.getRelatedUsersByRole("update");

    assertThat(result.getTotalRecords()).isEqualTo(1);
    assertThat(result.getRelatedUsers()).hasSize(1);
    assertThat(result.getRelatedUsers().get(0).getId()).isEqualTo(userId.toString());
    assertThat(result.getRelatedUsers().get(0).getFullName()).isEqualTo("Smith, Jane");
  }

  @Test
  void getRelatedUsersByRole_unknownRole_returnsEmptyCollection() {
    RelatedUserCollection result = userService.getRelatedUsersByRole("unknown");

    assertThat(result.getTotalRecords()).isZero();
    assertThat(result.getRelatedUsers()).isEmpty();
  }

  @Test
  void getRelatedUsersByRole_filtersOutUsersWithoutPersonal() {
    UUID userId = UUID.randomUUID();
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh();
    entity.setCreatedBy(userId);

    User userWithoutPersonal = new User(userId, Optional.empty());
    when(listRepository.findAll()).thenReturn(List.of(entity));
    when(usersClient.getUser(userId)).thenReturn(userWithoutPersonal);

    RelatedUserCollection result = userService.getRelatedUsersByRole("create");

    assertThat(result.getTotalRecords()).isZero();
    assertThat(result.getRelatedUsers()).isEmpty();
  }

  @Test
  void getRelatedUsersByRole_deduplicatesUsersWithSameId() {
    UUID userId = UUID.randomUUID();
    ListEntity entity1 = TestDataFixture.getListEntityWithSuccessRefresh(UUID.randomUUID());
    ListEntity entity2 = TestDataFixture.getListEntityWithSuccessRefresh(UUID.randomUUID());
    entity1.setCreatedBy(userId);
    entity2.setCreatedBy(userId);

    User user = new User(userId, Optional.of(new Personal("Alice", "Brown")));
    when(listRepository.findAll()).thenReturn(List.of(entity1, entity2));
    when(usersClient.getUser(userId)).thenReturn(user);

    RelatedUserCollection result = userService.getRelatedUsersByRole("create");

    assertThat(result.getTotalRecords()).isEqualTo(1);
    assertThat(result.getRelatedUsers()).hasSize(1);
  }

  @Test
  void getRelatedUsersByRole_create_skipsEntitiesWithNullCreatedBy() {
    ListEntity entityWithNull = TestDataFixture.getListEntityWithSuccessRefresh();
    entityWithNull.setCreatedBy(null);

    when(listRepository.findAll()).thenReturn(List.of(entityWithNull));

    RelatedUserCollection result = userService.getRelatedUsersByRole("create");

    assertThat(result.getTotalRecords()).isZero();
    assertThat(result.getRelatedUsers()).isEmpty();
  }

  @Test
  void getRelatedUsersByRole_create_returnsMultipleDistinctUsers() {
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();
    ListEntity entity1 = TestDataFixture.getListEntityWithSuccessRefresh(UUID.randomUUID());
    ListEntity entity2 = TestDataFixture.getListEntityWithSuccessRefresh(UUID.randomUUID());
    entity1.setCreatedBy(userId1);
    entity2.setCreatedBy(userId2);

    User user1 = new User(userId1, Optional.of(new Personal("Alice", "Brown")));
    User user2 = new User(userId2, Optional.of(new Personal("Bob", "Green")));
    when(listRepository.findAll()).thenReturn(List.of(entity1, entity2));
    when(usersClient.getUser(userId1)).thenReturn(user1);
    when(usersClient.getUser(userId2)).thenReturn(user2);

    RelatedUserCollection result = userService.getRelatedUsersByRole("create");

    assertThat(result.getTotalRecords()).isEqualTo(2);
    assertThat(result.getRelatedUsers()).hasSize(2);
  }
}
