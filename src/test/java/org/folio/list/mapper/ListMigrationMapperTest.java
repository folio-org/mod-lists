package org.folio.list.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;

import java.util.List;
import java.util.UUID;
import org.folio.list.domain.ListEntity;
import org.folio.list.utils.TestDataFixture;
import org.folio.querytool.domain.dto.FqmMigrateRequest;
import org.folio.querytool.domain.dto.FqmMigrateResponse;
import org.folio.querytool.domain.dto.FqmMigrateWarning;
import org.folio.spring.i18n.config.TranslationConfiguration;
import org.folio.spring.i18n.service.TranslationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = { ListMigrationMapperImpl.class, TranslationService.class, TranslationConfiguration.class })
class ListMigrationMapperTest {

  @Autowired
  private ListMigrationMapper mapper;

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
      stringContainsInOrder(
        sourceEntity.getDescription(),
        "This list was modified as part of a module upgrade",
        "produced the following warning:",
        "warning description"
      )
    );
  }

  @Test
  void testFromMigrationResponseWithMultipleWarnings() {
    ListEntity sourceEntity = TestDataFixture.getListEntityWithSuccessRefresh();

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
        "This list was modified as part of a module upgrade",
        "produced the following 3 warnings:",
        "first warning description",
        "second warning description",
        "third warning description"
      )
    );
  }
}
