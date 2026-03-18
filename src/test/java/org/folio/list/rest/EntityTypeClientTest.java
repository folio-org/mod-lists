package org.folio.list.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.list.exception.EntityTypeNotFoundException;
import org.folio.list.exception.InsufficientEntityTypePermissionsException;
import org.folio.list.services.ListActions;
import org.folio.list.util.TestDataFixture;
import org.folio.querytool.domain.dto.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
class EntityTypeClientTest {

  private static final EntityType ENTITY_TYPE = TestDataFixture.TEST_ENTITY_TYPE;
  private static final UUID ENTITY_TYPE_ID = UUID.fromString(ENTITY_TYPE.getId());

  @Spy
  private EntityTypeClient entityTypeClient;

  @Test
  void testReturnsOnSuccess() {
    when(entityTypeClient.getEntityType(ENTITY_TYPE_ID, false)).thenReturn(ENTITY_TYPE);

    assertThat(entityTypeClient.getEntityType(ENTITY_TYPE_ID, ListActions.READ, false), is(ENTITY_TYPE));
  }

  @Test
  void testHandlesUnauthorized() {
    when(entityTypeClient.getEntityType(ENTITY_TYPE_ID, false))
      .thenThrow(
        HttpClientErrorException.create(
          HttpStatus.UNAUTHORIZED,
          "Unauthorized",
          null,
          "[{\"User is missing permissions: [foo.bar]\"}]".getBytes(),
          null
        )
      );

    assertThrows(
      InsufficientEntityTypePermissionsException.class,
      () -> entityTypeClient.getEntityType(ENTITY_TYPE_ID, ListActions.READ, false)
    );
  }

  @Test
  void testHandlesNotFound() {
    when(entityTypeClient.getEntityType(ENTITY_TYPE_ID, false))
      .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

    assertThrows(
      EntityTypeNotFoundException.class,
      () -> entityTypeClient.getEntityType(ENTITY_TYPE_ID, ListActions.READ, false)
    );
  }
}
