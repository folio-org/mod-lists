package org.folio.list.domain;

import org.folio.list.rest.UsersClient;
import org.folio.list.utils.TestDataFixture;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListVersionTest {
  @Test
  void shouldSetDataFromListEntity() {
    ListEntity listEntity = TestDataFixture.getListEntityWithSuccessRefresh();
    UsersClient.User user = new UsersClient.User(UUID.randomUUID(), Optional.of(new UsersClient.Personal("Test", "User")));
    ListVersion listVersion = TestDataFixture.getListVersion();
    listVersion.setDataFromListEntity(listEntity, user);
    assertEquals(listVersion.getListId(), listEntity.getId());
    assertEquals(listVersion.getName(), listEntity.getName());
    assertEquals(listVersion.getFqlQuery(), listEntity.getFqlQuery());
    assertEquals(listVersion.getIsActive(), listEntity.getIsActive());
    assertEquals(listVersion.getUpdatedBy(), user.id());
    assertEquals(listVersion.getUpdatedByUsername(), user.getFullName().get());
  }
}
