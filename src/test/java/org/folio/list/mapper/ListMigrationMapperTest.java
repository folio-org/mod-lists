package org.folio.list.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.list.domain.ListEntity;
import org.folio.list.util.TestDataFixture;
import org.folio.querytool.domain.dto.FqmMigrateRequest;
import org.folio.querytool.domain.dto.FqmMigrateResponse;
import org.folio.querytool.domain.dto.FqmMigrateWarning;
import org.folio.spring.i18n.service.TranslationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListMigrationMapperTest {

  @Mock
  private TranslationService translationService;

  @InjectMocks
  private ListMigrationMapper mapper = new ListMigrationMapperImpl();

  @Test
  void testToMigrationRequest() {
    ListEntity sourceEntity = TestDataFixture.getListEntityWithSuccessRefresh();

    FqmMigrateRequest request = mapper.toMigrationRequest(sourceEntity);
    assertThat(request.getEntityTypeId(), is(sourceEntity.getEntityTypeId()));
    assertThat(request.getFqlQuery(), is(sourceEntity.getFqlQuery()));
    assertThat(request.getFields(), is(sourceEntity.getFields()));
  }

  @Test
  void testFromMigrationResponseNoWarnings() {
    ListEntity sourceEntity = TestDataFixture.getListEntityWithSuccessRefresh();

    FqmMigrateResponse response = new FqmMigrateResponse()
      .entityTypeId(UUID.fromString("7c3f9133-bda0-5206-944a-a7a2c2bbff80"))
      .fields(List.of("a", "b", "c"))
      .fqlQuery("new query")
      .warnings(List.of());

    ListEntity updatedEntity = mapper.updateListWithMigration(sourceEntity, response);
    assertThat(updatedEntity.getEntityTypeId(), is(response.getEntityTypeId()));
    assertThat(updatedEntity.getFqlQuery(), is(response.getFqlQuery()));
    assertThat(updatedEntity.getFields(), is(response.getFields()));
    assertThat(updatedEntity.getDescription(), is(sourceEntity.getDescription()));
  }

  @Test
  void testFromMigrationResponseWithOneWarning() {
    ListEntity sourceEntity = TestDataFixture.getListEntityWithSuccessRefresh();

    when(translationService.format(any(String.class), any(Object[].class)))
      .thenAnswer(invocation -> invocation.getArgument(0));

    FqmMigrateResponse response = new FqmMigrateResponse()
      .entityTypeId(UUID.fromString("7c3f9133-bda0-5206-944a-a7a2c2bbff80"))
      .fields(List.of("a", "b", "c"))
      .fqlQuery("new query")
      .warnings(List.of(new FqmMigrateWarning("warning description", "unused")));

    ListEntity updatedEntity = mapper.updateListWithMigration(sourceEntity, response);
    assertThat(updatedEntity.getEntityTypeId(), is(response.getEntityTypeId()));
    assertThat(updatedEntity.getFqlQuery(), is(response.getFqlQuery()));
    assertThat(updatedEntity.getFields(), is(response.getFields()));
    assertThat(
      updatedEntity.getDescription(),
      stringContainsInOrder(sourceEntity.getDescription(), "mod-lists.migration.warning-header", "warning description")
    );
  }

  /** @see https://folio-org.atlassian.net/browse/MODLISTS-180 */
  @Test
  void testFromMigrationResponseWithWarningAndNullOriginalDescription() {
    ListEntity sourceEntity = TestDataFixture.getListEntityWithSuccessRefresh().withDescription(null);

    when(translationService.format(any(String.class), any(Object[].class)))
      .thenAnswer(invocation -> invocation.getArgument(0));

    FqmMigrateResponse response = new FqmMigrateResponse()
      .entityTypeId(UUID.fromString("7c3f9133-bda0-5206-944a-a7a2c2bbff80"))
      .fields(List.of("a", "b", "c"))
      .fqlQuery("new query")
      .warnings(List.of(new FqmMigrateWarning("warning description", "unused")));

    ListEntity updatedEntity = mapper.updateListWithMigration(sourceEntity, response);
    assertThat(updatedEntity.getEntityTypeId(), is(response.getEntityTypeId()));
    assertThat(updatedEntity.getFqlQuery(), is(response.getFqlQuery()));
    assertThat(updatedEntity.getFields(), is(response.getFields()));
    assertThat(
      updatedEntity.getDescription(),
      allOf(
        stringContainsInOrder("mod-lists.migration.warning-header", "warning description"),
        startsWith("mod-lists.migration.warning-header"),
        not(stringContainsInOrder("null"))
      )
    );
  }

  @Test
  void testFromMigrationResponseWithMultipleWarnings() {
    ListEntity sourceEntity = TestDataFixture.getListEntityWithSuccessRefresh();

    when(translationService.format(any(String.class), any(Object[].class)))
      .thenAnswer(invocation -> invocation.getArgument(0));

    FqmMigrateResponse response = new FqmMigrateResponse()
      .entityTypeId(UUID.fromString("7c3f9133-bda0-5206-944a-a7a2c2bbff80"))
      .fields(List.of("a", "b", "c"))
      .fqlQuery("new query")
      .warnings(
        List.of(
          new FqmMigrateWarning("first warning description", "unused"),
          new FqmMigrateWarning("second warning description", "unused"),
          new FqmMigrateWarning("third warning description", "unused")
        )
      );

    ListEntity updatedEntity = mapper.updateListWithMigration(sourceEntity, response);
    assertThat(updatedEntity.getEntityTypeId(), is(response.getEntityTypeId()));
    assertThat(updatedEntity.getFqlQuery(), is(response.getFqlQuery()));
    assertThat(updatedEntity.getFields(), is(response.getFields()));
    assertThat(
      updatedEntity.getDescription(),
      stringContainsInOrder(
        sourceEntity.getDescription(),
        "mod-lists.migration.warning-header",
        "first warning description",
        "second warning description",
        "third warning description"
      )
    );
  }
}
