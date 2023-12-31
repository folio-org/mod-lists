package org.folio.list.service;

import org.folio.fql.model.*;
import org.folio.list.rest.QueryClient;
import org.folio.list.services.UserFriendlyQueryService;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.querytool.domain.dto.SourceColumn;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFriendlyQueryServiceTest {

  @InjectMocks
  private UserFriendlyQueryService userFriendlyQueryService;
  @Mock
  private QueryClient queryClient;

  @Test
  void shouldGetStringForFqlEqualsConditionWithoutIdColumn() {
    EntityType entityType = new EntityType();
    EqualsCondition equalsCondition = new EqualsCondition("field1", "some value");
    String expectedEqualsCondition = "field1 == some value";
    String actualEqualsConditions = userFriendlyQueryService.getUserFriendlyQuery(equalsCondition, entityType);
    assertEquals(expectedEqualsCondition, actualEqualsConditions);
  }

  @Test
  void shouldGetStringForFqlEqualsConditionWithIdColumn() {
    List<Map<String, Object>> entityContents = List.of(
      Map.of("field1", "some value")
    );
    UUID sourceEntityTypeId = UUID.randomUUID();
    UUID value = UUID.randomUUID();
    UUID entityTypeId = UUID.randomUUID();
    List<String> fields = List.of("id", "field1");

    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId.toString()).columnName("field1");
    EntityTypeColumn column = new EntityTypeColumn().name("field1");
    EntityTypeColumn column1 = new EntityTypeColumn().name("field2").idColumnName("field1").source(sourceColumn);
    EntityType entityType = new EntityType().id(entityTypeId.toString()).columns(List.of(column, column1));
    EqualsCondition equalsCondition = new EqualsCondition("field1", value.toString());
    List<UUID> ids = List.of(UUID.fromString(equalsCondition.value().toString()));
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(sourceEntityTypeId)
      .fields(fields)
      .ids(ids);

    when(queryClient.getContents(contentsRequest)).thenReturn(entityContents);

    String expectedEqualsCondition = "field2 == some value";
    String actualEqualsConditions = userFriendlyQueryService.getUserFriendlyQuery(equalsCondition, entityType);
    assertEquals(expectedEqualsCondition, actualEqualsConditions);
  }

  @Test
  void shouldGetStringForFqlNotEqualsConditionWithoutIdColumn() {
    EntityType entityType = new EntityType();
    NotEqualsCondition notEqualsCondition = new NotEqualsCondition("field1", "some value");
    String expectedNotEqualsCondition = "field1 != some value";
    String actualNotEqualsConditions = userFriendlyQueryService.getUserFriendlyQuery(notEqualsCondition, entityType);
    assertEquals(expectedNotEqualsCondition, actualNotEqualsConditions);
  }

  @Test
  void shouldGetStringForFqlNotEqualsConditionWithIdColumn() {
    List<Map<String, Object>> entityContents = List.of(
      Map.of("field1", "some value")
    );
    UUID entityTypeId = UUID.randomUUID();
    UUID sourceEntityTypeId = UUID.randomUUID();
    UUID value = UUID.randomUUID();
    List<String> fields = List.of("id", "field1");
    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId.toString()).columnName("field1");
    EntityTypeColumn column = new EntityTypeColumn().name("field1");
    EntityTypeColumn column1 = new EntityTypeColumn().name("field2").idColumnName("field1").source(sourceColumn);
    EntityType entityType = new EntityType().id(entityTypeId.toString()).columns(List.of(column, column1));
    NotEqualsCondition notEqualsCondition = new NotEqualsCondition("field1", value.toString());
    List<UUID> ids = List.of(UUID.fromString(notEqualsCondition.value().toString()));
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(sourceEntityTypeId)
      .fields(fields)
      .ids(ids);

    when(queryClient.getContents(contentsRequest)).thenReturn(entityContents);

    String expectedNotEqualsCondition = "field2 != some value";
    String actualNotEqualsConditions = userFriendlyQueryService.getUserFriendlyQuery(notEqualsCondition, entityType);
    assertEquals(expectedNotEqualsCondition, actualNotEqualsConditions);
  }

  @Test
  void shouldGetStringForFqlInConditionWithoutIdColumn() {
    EntityType entityType = new EntityType();
    InCondition inCondition = new InCondition("field1", List.of("value1", "value2"));
    String expectedInCondition = "field1 in [value1, value2]";
    String actualInCondition = userFriendlyQueryService.getUserFriendlyQuery(inCondition, entityType);
    assertEquals(expectedInCondition, actualInCondition);
  }

  @Test
  void shouldGetStringForFqlInConditionWithIdColumn() {
    List<Map<String, Object>> entityContents = List.of(
      Map.of("field1", "value1"),
      Map.of("field1", "value2")
    );
    UUID entityTypeId = UUID.randomUUID();
    UUID sourceEntityTypeId = UUID.randomUUID();
    UUID value1 = UUID.randomUUID();
    UUID value2 = UUID.randomUUID();
    List<UUID> ids = List.of(value1, value2);
    List<String> fields = List.of("id", "field1");
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(sourceEntityTypeId)
      .fields(fields)
      .ids(ids);
    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId.toString()).columnName("field1");
    EntityTypeColumn column = new EntityTypeColumn().name("field1");
    EntityTypeColumn column1 = new EntityTypeColumn().name("field2").idColumnName("field1").source(sourceColumn);
    EntityType entityType = new EntityType().id(entityTypeId.toString()).columns(List.of(column, column1));
    InCondition inCondition = new InCondition("field1", List.of(value1, value2));

    when(queryClient.getContents(contentsRequest)).thenReturn(entityContents);

    String expectedInCondition = "field2 in [value1, value2]";
    String actualInConditions = userFriendlyQueryService.getUserFriendlyQuery(inCondition, entityType);
    assertEquals(expectedInCondition, actualInConditions);
  }

  @Test
  void shouldGetStringForFqlNotInConditionWithoutIdColumn() {
    EntityType entityType = new EntityType();
    NotInCondition notInCondition = new NotInCondition("field1", List.of("value1", "value2"));
    String expectedNotInCondition = "field1 not in [value1, value2]";
    String actualNotInCondition = userFriendlyQueryService.getUserFriendlyQuery(notInCondition, entityType);
    assertEquals(expectedNotInCondition, actualNotInCondition);
  }

  @Test
  void shouldGetStringForFqlNotInConditionWithIdColumn() {
    List<Map<String, Object>> entityContents = List.of(
      Map.of("field1", "value1"),
      Map.of("field1", "value2")
    );
    UUID entityTypeId = UUID.randomUUID();
    UUID sourceEntityTypeId = UUID.randomUUID();
    UUID value1 = UUID.randomUUID();
    UUID value2 = UUID.randomUUID();
    List<UUID> ids = List.of(value1, value2);
    List<String> fields = List.of("id", "field1");
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(sourceEntityTypeId)
      .fields(fields)
      .ids(ids);
    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId.toString()).columnName("field1");
    EntityTypeColumn column = new EntityTypeColumn().name("field1");
    EntityTypeColumn column1 = new EntityTypeColumn().name("field2").idColumnName("field1").source(sourceColumn);
    EntityType entityType = new EntityType().id(entityTypeId.toString()).columns(List.of(column, column1));
    NotInCondition notInCondition = new NotInCondition("field1", List.of(value1, value2));

    when(queryClient.getContents(contentsRequest)).thenReturn(entityContents);

    String expectedNotInCondition = "field2 not in [value1, value2]";
    String actualNotInCondition = userFriendlyQueryService.getUserFriendlyQuery(notInCondition, entityType);
    assertEquals(expectedNotInCondition, actualNotInCondition);
  }

  @Test
  void shouldGetStringForFqlContainsConditionWithoutIdColumn() {
    EntityType entityType = new EntityType();
    ContainsCondition containsCondition = new ContainsCondition("field1", "value1");
    String expectedContainsCondition = "field1 contains value1";
    String actualContainsCondition = userFriendlyQueryService.getUserFriendlyQuery(containsCondition, entityType);
    assertEquals(expectedContainsCondition, actualContainsCondition);
  }

  @Test
  void shouldGetStringForFqlContainsConditionWithIdColumn() {
    List<Map<String, Object>> entityContents = List.of(
      Map.of("field1", "value1")
    );
    UUID sourceEntityTypeId = UUID.randomUUID();
    UUID value = UUID.randomUUID();
    UUID entityTypeId = UUID.randomUUID();
    List<String> fields = List.of("id", "field1");

    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId.toString()).columnName("field1");
    EntityTypeColumn column = new EntityTypeColumn().name("field1");
    EntityTypeColumn column1 = new EntityTypeColumn().name("field2").idColumnName("field1").source(sourceColumn);
    EntityType entityType = new EntityType().id(entityTypeId.toString()).columns(List.of(column, column1));
    ContainsCondition containsCondition = new ContainsCondition("field1", value.toString());
    List<UUID> ids = List.of(UUID.fromString(containsCondition.value().toString()));
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(sourceEntityTypeId)
      .fields(fields)
      .ids(ids);

    when(queryClient.getContents(contentsRequest)).thenReturn(entityContents);

    String expectedContainsCondition = "field2 contains value1";
    String actualContainsConditions = userFriendlyQueryService.getUserFriendlyQuery(containsCondition, entityType);
    assertEquals(expectedContainsCondition, actualContainsConditions);
  }

  @Test
  void shouldGetStringForFqlNotContainsConditionWithoutIdColumn() {
    EntityType entityType = new EntityType();
    NotContainsCondition notContainsCondition = new NotContainsCondition("field1", "value1");
    String expectedNotContainsCondition = "field1 does not contain value1";
    String actualNotContainsCondition = userFriendlyQueryService.getUserFriendlyQuery(notContainsCondition, entityType);
    assertEquals(expectedNotContainsCondition, actualNotContainsCondition);
  }

  @Test
  void shouldGetStringForFqlNotContainsConditionWithIdColumn() {
    List<Map<String, Object>> entityContents = List.of(
      Map.of("field1", "value1")
    );
    UUID sourceEntityTypeId = UUID.randomUUID();
    UUID value = UUID.randomUUID();
    UUID entityTypeId = UUID.randomUUID();
    List<String> fields = List.of("id", "field1");

    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId.toString()).columnName("field1");
    EntityTypeColumn column = new EntityTypeColumn().name("field1");
    EntityTypeColumn column1 = new EntityTypeColumn().name("field2").idColumnName("field1").source(sourceColumn);
    EntityType entityType = new EntityType().id(entityTypeId.toString()).columns(List.of(column, column1));
    NotContainsCondition notContainsCondition = new NotContainsCondition("field1", value.toString());
    List<UUID> ids = List.of(UUID.fromString(notContainsCondition.value().toString()));
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(sourceEntityTypeId)
      .fields(fields)
      .ids(ids);

    when(queryClient.getContents(contentsRequest)).thenReturn(entityContents);

    String expectedNotContainsCondition = "field2 does not contain value1";
    String actualNotContainsConditions = userFriendlyQueryService.getUserFriendlyQuery(notContainsCondition, entityType);
    assertEquals(expectedNotContainsCondition, actualNotContainsConditions);
  }

  @Test
  void shouldGetStringForFqlGreaterThanCondition() {
    EntityType entityType = new EntityType();
    GreaterThanCondition greaterThanCondition = new GreaterThanCondition("field1", false, "some value");
    String expectedGreaterThanCondition = "field1 > some value";
    String actualGreaterThanCondition = userFriendlyQueryService.getUserFriendlyQuery(greaterThanCondition, entityType);
    assertEquals(expectedGreaterThanCondition, actualGreaterThanCondition);
  }

  @Test
  void shouldGetStringForFqlGreaterThanEqualToCondition() {
    EntityType entityType = new EntityType();
    GreaterThanCondition greaterThanEqualCondition = new GreaterThanCondition("field1", true, "some value");
    String expectedGreaterThanEqualCondition = "field1 >= some value";
    String actualGreaterThanEqualCondition = userFriendlyQueryService.getUserFriendlyQuery(greaterThanEqualCondition, entityType);
    assertEquals(expectedGreaterThanEqualCondition, actualGreaterThanEqualCondition);
  }

  @Test
  void shouldGetStringForFqlLessThanCondition() {
    EntityType entityType = new EntityType();
    LessThanCondition lessThanCondition = new LessThanCondition("field1", false, "some value");
    String expectedLessThanCondition = "field1 < some value";
    String actualLessThanCondition = userFriendlyQueryService.getUserFriendlyQuery(lessThanCondition, entityType);
    assertEquals(expectedLessThanCondition, actualLessThanCondition);
  }

  @Test
  void shouldGetStringForFqlLessThanEqualToCondition() {
    EntityType entityType = new EntityType();
    LessThanCondition lessThanEqualCondition = new LessThanCondition("field1", true, "some value");
    String expectedLessThanEqualCondition = "field1 <= some value";
    String actualLessThanEqualCondition = userFriendlyQueryService.getUserFriendlyQuery(lessThanEqualCondition, entityType);
    assertEquals(expectedLessThanEqualCondition, actualLessThanEqualCondition);
  }

  @Test
  void shouldGetStringForFqlRegexStartsWithCondition() {
    EntityType entityType = new EntityType();
    RegexCondition regexCondition = new RegexCondition("field1", "^some value");
    String expectedRegexCondition = "field1 starts with some value";
    String actualRegexCondition = userFriendlyQueryService.getUserFriendlyQuery(regexCondition, entityType);
    assertEquals(expectedRegexCondition, actualRegexCondition);
  }

  @Test
  void shouldGetStringForFqlRegexContainsCondition() {
    EntityType entityType = new EntityType();
    RegexCondition regexCondition = new RegexCondition("field1", "some value");
    String expectedRegexCondition = "field1 contains some value";
    String actualRegexCondition = userFriendlyQueryService.getUserFriendlyQuery(regexCondition, entityType);
    assertEquals(expectedRegexCondition, actualRegexCondition);
  }

  @Test
  void shouldGetStringForFqlAndCondition() {
    EntityType entityType = new EntityType();
    RegexCondition regexCondition = new RegexCondition("field1", "^some value");
    LessThanCondition lessThanCondition = new LessThanCondition("field1", false, "some value");
    AndCondition andCondition = new AndCondition(List.of(regexCondition, lessThanCondition));
    String expectedAndCondition = "(field1 starts with some value) AND (field1 < some value)";
    String actualAndCondition = userFriendlyQueryService.getUserFriendlyQuery(andCondition, entityType);
    assertEquals(expectedAndCondition, actualAndCondition);
  }
}
