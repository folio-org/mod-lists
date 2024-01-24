package org.folio.list.mapper;

import lombok.SneakyThrows;
import org.folio.list.domain.ListEntity;
import org.folio.list.rest.UsersClient;
import org.folio.list.utils.TestDataFixture;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(classes = ListEntityMapperImpl.class)
class ListEntityMapperTest {
  @Autowired
  private ListEntityMapper listEntityMapper;

  @Mock
  private UsersClient usersClient;

  @Test
  @SneakyThrows
  void shouldMapDtoToEntity() {
    var listRequestDto = TestDataFixture.getListRequestDTO();
    UUID userId = UUID.randomUUID();
    UsersClient.User user = new UsersClient.User(userId, Optional.of(new UsersClient.Personal("firstname", "lastname")));
    ListEntity listEntity = listEntityMapper.toListEntity(listRequestDto, user);

    assertNotNull(listEntity.getId());
    assertEquals(listRequestDto.getName(), listEntity.getName());
    assertEquals(listRequestDto.getEntityTypeId(), listEntity.getEntityTypeId());
    assertEquals(listRequestDto.getDescription(), listEntity.getDescription());
    assertEquals(listRequestDto.getFqlQuery(), listEntity.getFqlQuery());
    assertEquals(listRequestDto.getIsActive(), listEntity.getIsActive());
    assertEquals(listRequestDto.getIsPrivate(), listEntity.getIsPrivate());
    assertEquals(user.getFullName().get(), listEntity.getCreatedByUsername());
    assertEquals(user.id(), listEntity.getCreatedBy());
    assertNotNull(listEntity.getCreatedDate());
    assertFalse(listEntity.getIsCanned());
    assertFalse(listEntity.getIsDeleted());
  }
}
