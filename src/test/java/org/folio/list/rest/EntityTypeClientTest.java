package org.folio.list.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import feign.FeignException;
import java.util.UUID;
import org.folio.list.exception.InsufficientEntityTypePermissionsException;
import org.folio.list.services.ListActions;
import org.folio.list.util.TestDataFixture;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

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
        new FeignException.Unauthorized(
          "[{\"User is missing permissions: [foo.bar]\"}]",
          mock(feign.Request.class),
          null,
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
      .thenThrow(new FeignException.NotFound("Entity type not found", mock(feign.Request.class), null, null));

    assertThrows(NotFoundException.class, () -> entityTypeClient.getEntityType(ENTITY_TYPE_ID, ListActions.READ, false));
  }
}
