package org.folio.list.service;

import org.folio.fql.model.AndCondition;
import org.folio.fql.model.ContainsAllCondition;
import org.folio.fql.model.ContainsAnyCondition;
import org.folio.fql.model.EmptyCondition;
import org.folio.fql.model.EqualsCondition;
import org.folio.fql.model.GreaterThanCondition;
import org.folio.fql.model.InCondition;
import org.folio.fql.model.LessThanCondition;
import org.folio.fql.model.NotContainsAllCondition;
import org.folio.fql.model.NotContainsAnyCondition;
import org.folio.fql.model.NotEqualsCondition;
import org.folio.fql.model.NotInCondition;
import org.folio.fql.model.RegexCondition;
import org.folio.fql.model.field.FqlField;
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
    EqualsCondition equalsCondition = new EqualsCondition(new FqlField("field1"), "some value");
    String expectedEqualsCondition = "field1 == some value";
    String actualEqualsConditions = userFriendlyQueryService.getUserFriendlyQuery(equalsCondition, entityType);
    assertEquals(expectedEqualsCondition, actualEqualsConditions);
  }

  /**
   * Indirectly querying an underlying ID column should continue to use the "nice" column, with proper value resolution
   * (query of underlying == ID should map to friendly == some value)
   */
  @Test
  void shouldGetStringForFqlEqualsConditionWithIdColumnIndirectlyReferenced() {
    UUID entityTypeId = new UUID(0, 0);
    UUID sourceEntityTypeId = new UUID(0, 1);
    UUID searchValue = new UUID(0, 2);

    EntityType entityType = new EntityType()
      .id(entityTypeId.toString())
      .columns(List.of(
        new EntityTypeColumn().name("underlying"),
        new EntityTypeColumn().name("friendly")
          .idColumnName("underlying")
          .source(new SourceColumn().entityTypeId(sourceEntityTypeId.toString()).columnName("sourceField"))
      ));
    EqualsCondition equalsCondition = new EqualsCondition(new FqlField("friendly"), searchValue.toString());

    List<String> fields = List.of("id", "sourceField");
    List<List<String>> ids = List.of(List.of(searchValue.toString()));
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(sourceEntityTypeId)
      .fields(fields)
      .ids(ids);
    List<Map<String, Object>> sourceEntityContents = List.of(
      Map.of("sourceField", "some value")
    );
    when(queryClient.getContents(contentsRequest)).thenReturn(sourceEntityContents);

    String expectedEqualsCondition = "friendly == some value";
    String actualEqualsConditions = userFriendlyQueryService.getUserFriendlyQuery(equalsCondition, entityType);
    assertEquals(expectedEqualsCondition, actualEqualsConditions);
  }

  /**
   * Directly querying an underlying ID column should get back to the original, "nice" column
   * (query of underlying == ID should map to friendly == some value)
   */
  @Test
  void shouldGetStringForFqlEqualsConditionWithIdColumnDirectlyReferenced() {
    UUID entityTypeId = new UUID(0, 0);
    UUID sourceEntityTypeId = new UUID(0, 1);
    UUID searchValue = new UUID(0, 2);

    EntityType entityType = new EntityType()
      .id(entityTypeId.toString())
      .columns(List.of(
        new EntityTypeColumn().name("underlying"),
        new EntityTypeColumn().name("friendly")
          .idColumnName("underlying")
          .source(new SourceColumn().entityTypeId(sourceEntityTypeId.toString()).columnName("sourceField"))
      ));
    EqualsCondition equalsCondition = new EqualsCondition(new FqlField("underlying"), searchValue.toString());

    List<String> fields = List.of("id", "sourceField");
    List<List<String>> ids = List.of(List.of(searchValue.toString()));
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(sourceEntityTypeId)
      .fields(fields)
      .ids(ids);
    List<Map<String, Object>> sourceEntityContents = List.of(
      Map.of("sourceField", "some value")
    );
    when(queryClient.getContents(contentsRequest)).thenReturn(sourceEntityContents);

    String expectedEqualsCondition = "friendly == some value";
    String actualEqualsConditions = userFriendlyQueryService.getUserFriendlyQuery(equalsCondition, entityType);
    assertEquals(expectedEqualsCondition, actualEqualsConditions);
  }

  @Test
  void shouldGetStringForFqlNotEqualsConditionWithoutIdColumn() {
    EntityType entityType = new EntityType();
    NotEqualsCondition notEqualsCondition = new NotEqualsCondition(new FqlField("field1"), "some value");
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
    NotEqualsCondition notEqualsCondition = new NotEqualsCondition(new FqlField("field1"), value.toString());
    List<List<String>> ids = List.of(
      List.of(notEqualsCondition.value().toString())
    );
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
    InCondition inCondition = new InCondition(new FqlField("field1"), List.of("value1", "value2"));
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
    List<List<String>> ids = List.of(
      List.of(value1.toString()),
      List.of(value2.toString())
    );
    List<String> fields = List.of("id", "field1");
    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId.toString()).columnName("field1");
    EntityTypeColumn column = new EntityTypeColumn().name("field1");
    EntityTypeColumn column1 = new EntityTypeColumn().name("field2").idColumnName("field1").source(sourceColumn);
    EntityType entityType = new EntityType().id(entityTypeId.toString()).columns(List.of(column, column1));
    InCondition inCondition = new InCondition(new FqlField("field1"), List.of(value1.toString(), value2.toString()));
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(sourceEntityTypeId)
      .fields(fields)
      .ids(ids);

    when(queryClient.getContents(contentsRequest)).thenReturn(entityContents);

    String expectedInCondition = "field2 in [value1, value2]";
    String actualInConditions = userFriendlyQueryService.getUserFriendlyQuery(inCondition, entityType);
    assertEquals(expectedInCondition, actualInConditions);
  }

  @Test
  void shouldGetStringForFqlNotInConditionWithoutIdColumn() {
    EntityType entityType = new EntityType();
    NotInCondition notInCondition = new NotInCondition(new FqlField("field1"), List.of("value1", "value2"));
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
    List<List<String>> ids = List.of(
      List.of(value1.toString()),
      List.of(value2.toString())
    );
    List<String> fields = List.of("id", "field1");
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(sourceEntityTypeId)
      .fields(fields)
      .ids(ids);
    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId.toString()).columnName("field1");
    EntityTypeColumn column = new EntityTypeColumn().name("field1");
    EntityTypeColumn column1 = new EntityTypeColumn().name("field2").idColumnName("field1").source(sourceColumn);
    EntityType entityType = new EntityType().id(entityTypeId.toString()).columns(List.of(column, column1));
    NotInCondition notInCondition = new NotInCondition(new FqlField("field1"), List.of(value1, value2));

    when(queryClient.getContents(contentsRequest)).thenReturn(entityContents);

    String expectedNotInCondition = "field2 not in [value1, value2]";
    String actualNotInCondition = userFriendlyQueryService.getUserFriendlyQuery(notInCondition, entityType);
    assertEquals(expectedNotInCondition, actualNotInCondition);
  }

  @Test
  void shouldGetStringForFqlContainsAllConditionWithoutIdColumn() {
    EntityType entityType = new EntityType();
    ContainsAllCondition containsAllCondition = new ContainsAllCondition(new FqlField("field1"), List.of("value1"));
    String expectedContainsAllCondition = "field1 contains all [value1]";
    String actualContainsAllCondition = userFriendlyQueryService.getUserFriendlyQuery(containsAllCondition, entityType);
    assertEquals(expectedContainsAllCondition, actualContainsAllCondition);
  }

  @Test
  void shouldGetStringForFqlContainsAllConditionWithIdColumn() {
    List<Map<String, Object>> entityContents = List.of(
      Map.of("field1", "value1"),
      Map.of("field1", "value2")
    );
    UUID entityTypeId = UUID.randomUUID();
    UUID sourceEntityTypeId = UUID.randomUUID();
    UUID value1 = UUID.randomUUID();
    UUID value2 = UUID.randomUUID();
    List<List<String>> ids = List.of(
      List.of(value1.toString()),
      List.of(value2.toString())
    );
    List<String> fields = List.of("id", "field1");
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(sourceEntityTypeId)
      .fields(fields)
      .ids(ids);
    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId.toString()).columnName("field1");
    EntityTypeColumn column = new EntityTypeColumn().name("field1");
    EntityTypeColumn column1 = new EntityTypeColumn().name("field2").idColumnName("field1").source(sourceColumn);
    EntityType entityType = new EntityType().id(entityTypeId.toString()).columns(List.of(column, column1));
    ContainsAllCondition containsAllCondition = new ContainsAllCondition(new FqlField("field1"), List.of(value1, value2));

    when(queryClient.getContents(contentsRequest)).thenReturn(entityContents);

    String expectedContainsAllCondition = "field2 contains all [value1, value2]";
    String actualContainsAllCondition = userFriendlyQueryService.getUserFriendlyQuery(containsAllCondition, entityType);
    assertEquals(expectedContainsAllCondition, actualContainsAllCondition);
  }

  @Test
  void shouldGetStringForFqlContainsAnyConditionWithoutIdColumn() {
    EntityType entityType = new EntityType();
    ContainsAnyCondition containsAnyCondition = new ContainsAnyCondition(new FqlField("field1"), List.of("value1"));
    String expectedContainsAnyCondition = "field1 contains any [value1]";
    String actualContainsAnyCondition = userFriendlyQueryService.getUserFriendlyQuery(containsAnyCondition, entityType);
    assertEquals(expectedContainsAnyCondition, actualContainsAnyCondition);
  }

  @Test
  void shouldGetStringForFqlContainsAnyConditionWithIdColumn() {
    List<Map<String, Object>> entityContents = List.of(
      Map.of("field1", "value1"),
      Map.of("field1", "value2")
    );
    UUID entityTypeId = UUID.randomUUID();
    UUID sourceEntityTypeId = UUID.randomUUID();
    UUID value1 = UUID.randomUUID();
    UUID value2 = UUID.randomUUID();
    List<List<String>> ids = List.of(
      List.of(value1.toString()),
      List.of(value2.toString())
    );
    List<String> fields = List.of("id", "field1");
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(sourceEntityTypeId)
      .fields(fields)
      .ids(ids);
    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId.toString()).columnName("field1");
    EntityTypeColumn column = new EntityTypeColumn().name("field1");
    EntityTypeColumn column1 = new EntityTypeColumn().name("field2").idColumnName("field1").source(sourceColumn);
    EntityType entityType = new EntityType().id(entityTypeId.toString()).columns(List.of(column, column1));
    ContainsAnyCondition containsAnyCondition = new ContainsAnyCondition(new FqlField("field1"), List.of(value1, value2));

    when(queryClient.getContents(contentsRequest)).thenReturn(entityContents);

    String expectedContainsAnyCondition = "field2 contains any [value1, value2]";
    String actualContainsAnyCondition = userFriendlyQueryService.getUserFriendlyQuery(containsAnyCondition, entityType);
    assertEquals(expectedContainsAnyCondition, actualContainsAnyCondition);
  }



  @Test
  void shouldGetStringForFqlNotContainsAllConditionWithoutIdColumn() {
    EntityType entityType = new EntityType();
    NotContainsAllCondition notContainsAllCondition = new NotContainsAllCondition(new FqlField("field1"), List.of("value1"));
    String expectedNotContainsAllCondition = "field1 does not contain all [value1]";
    String actualNotContainsAllCondition = userFriendlyQueryService.getUserFriendlyQuery(notContainsAllCondition, entityType);
    assertEquals(expectedNotContainsAllCondition, actualNotContainsAllCondition);
  }

  @Test
  void shouldGetStringForFqlNotContainsAllConditionWithIdColumn() {
    List<Map<String, Object>> entityContents = List.of(
      Map.of("field1", "value1"),
      Map.of("field1", "value2")
    );
    UUID entityTypeId = UUID.randomUUID();
    UUID sourceEntityTypeId = UUID.randomUUID();
    UUID value1 = UUID.randomUUID();
    UUID value2 = UUID.randomUUID();
    List<List<String>> ids = List.of(
      List.of(value1.toString()),
      List.of(value2.toString())
    );
    List<String> fields = List.of("id", "field1");
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(sourceEntityTypeId)
      .fields(fields)
      .ids(ids);
    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId.toString()).columnName("field1");
    EntityTypeColumn column = new EntityTypeColumn().name("field1");
    EntityTypeColumn column1 = new EntityTypeColumn().name("field2").idColumnName("field1").source(sourceColumn);
    EntityType entityType = new EntityType().id(entityTypeId.toString()).columns(List.of(column, column1));
    NotContainsAllCondition notContainsAllCondition = new NotContainsAllCondition(new FqlField("field1"), List.of(value1, value2));

    when(queryClient.getContents(contentsRequest)).thenReturn(entityContents);

    String expectedNotContainsAllCondition = "field2 does not contain all [value1, value2]";
    String actualNotContainsAllCondition = userFriendlyQueryService.getUserFriendlyQuery(notContainsAllCondition, entityType);
    assertEquals(expectedNotContainsAllCondition, actualNotContainsAllCondition);
  }

  @Test
  void shouldGetStringForFqlNotContainsAnyConditionWithoutIdColumn() {
    EntityType entityType = new EntityType();
    NotContainsAnyCondition notContainsAnyCondition = new NotContainsAnyCondition(new FqlField("field1"), List.of("value1"));
    String expectedNotContainsAnyCondition = "field1 does not contain any [value1]";
    String actualNotContainsAnyCondition = userFriendlyQueryService.getUserFriendlyQuery(notContainsAnyCondition, entityType);
    assertEquals(expectedNotContainsAnyCondition, actualNotContainsAnyCondition);
  }

  @Test
  void shouldGetStringForFqlNotContainsAnyConditionWithIdColumn() {
    List<Map<String, Object>> entityContents = List.of(
      Map.of("field1", "value1"),
      Map.of("field1", "value2")
    );
    UUID entityTypeId = UUID.randomUUID();
    UUID sourceEntityTypeId = UUID.randomUUID();
    UUID value1 = UUID.randomUUID();
    UUID value2 = UUID.randomUUID();
    List<List<String>> ids = List.of(
      List.of(value1.toString()),
      List.of(value2.toString())
    );
    List<String> fields = List.of("id", "field1");
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(sourceEntityTypeId)
      .fields(fields)
      .ids(ids);
    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId.toString()).columnName("field1");
    EntityTypeColumn column = new EntityTypeColumn().name("field1");
    EntityTypeColumn column1 = new EntityTypeColumn().name("field2").idColumnName("field1").source(sourceColumn);
    EntityType entityType = new EntityType().id(entityTypeId.toString()).columns(List.of(column, column1));
    NotContainsAnyCondition notContainsAnyCondition = new NotContainsAnyCondition(new FqlField("field1"), List.of(value1, value2));

    when(queryClient.getContents(contentsRequest)).thenReturn(entityContents);

    String expectedNotContainsAnyCondition = "field2 does not contain any [value1, value2]";
    String actualNotContainsAnyCondition = userFriendlyQueryService.getUserFriendlyQuery(notContainsAnyCondition, entityType);
    assertEquals(expectedNotContainsAnyCondition, actualNotContainsAnyCondition);
  }

  @Test
  void shouldGetStringForFqlGreaterThanCondition() {
    EntityType entityType = new EntityType();
    GreaterThanCondition greaterThanCondition = new GreaterThanCondition(new FqlField("field1"), false, "some value");
    String expectedGreaterThanCondition = "field1 > some value";
    String actualGreaterThanCondition = userFriendlyQueryService.getUserFriendlyQuery(greaterThanCondition, entityType);
    assertEquals(expectedGreaterThanCondition, actualGreaterThanCondition);
  }

  @Test
  void shouldGetStringForFqlGreaterThanEqualToCondition() {
    EntityType entityType = new EntityType();
    GreaterThanCondition greaterThanEqualCondition = new GreaterThanCondition(new FqlField("field1"), true, "some value");
    String expectedGreaterThanEqualCondition = "field1 >= some value";
    String actualGreaterThanEqualCondition = userFriendlyQueryService.getUserFriendlyQuery(greaterThanEqualCondition, entityType);
    assertEquals(expectedGreaterThanEqualCondition, actualGreaterThanEqualCondition);
  }

  @Test
  void shouldGetStringForFqlLessThanCondition() {
    EntityType entityType = new EntityType();
    LessThanCondition lessThanCondition = new LessThanCondition(new FqlField("field1"), false, "some value");
    String expectedLessThanCondition = "field1 < some value";
    String actualLessThanCondition = userFriendlyQueryService.getUserFriendlyQuery(lessThanCondition, entityType);
    assertEquals(expectedLessThanCondition, actualLessThanCondition);
  }

  @Test
  void shouldGetStringForFqlLessThanEqualToCondition() {
    EntityType entityType = new EntityType();
    LessThanCondition lessThanEqualCondition = new LessThanCondition(new FqlField("field1"), true, "some value");
    String expectedLessThanEqualCondition = "field1 <= some value";
    String actualLessThanEqualCondition = userFriendlyQueryService.getUserFriendlyQuery(lessThanEqualCondition, entityType);
    assertEquals(expectedLessThanEqualCondition, actualLessThanEqualCondition);
  }

  @Test
  void shouldGetStringForFqlRegexStartsWithCondition() {
    EntityType entityType = new EntityType();
    RegexCondition regexCondition = new RegexCondition(new FqlField("field1"), "^some value");
    String expectedRegexCondition = "field1 starts with some value";
    String actualRegexCondition = userFriendlyQueryService.getUserFriendlyQuery(regexCondition, entityType);
    assertEquals(expectedRegexCondition, actualRegexCondition);
  }

  @Test
  void shouldGetStringForFqlRegexContainsCondition() {
    EntityType entityType = new EntityType();
    RegexCondition regexCondition = new RegexCondition(new FqlField("field1"), "some value");
    String expectedRegexCondition = "field1 contains some value";
    String actualRegexCondition = userFriendlyQueryService.getUserFriendlyQuery(regexCondition, entityType);
    assertEquals(expectedRegexCondition, actualRegexCondition);
  }

  @Test
  void shouldGetStringForFqlEmptyCondition() {
    EntityType entityType = new EntityType();
    EmptyCondition emptyCondition = new EmptyCondition(new FqlField("field1"), true);
    String expectedEmptyCondition = "field1 is empty";
    String actualRegexCondition = userFriendlyQueryService.getUserFriendlyQuery(emptyCondition, entityType);
    assertEquals(expectedEmptyCondition, actualRegexCondition);
  }

  @Test
  void shouldGetStringForFqlNotEmptyCondition() {
    EntityType entityType = new EntityType();
    EmptyCondition notEmptyCondition = new EmptyCondition(new FqlField("field1"), false);
    String expectedNotEmptyCondition = "field1 is not empty";
    String actualRegexCondition = userFriendlyQueryService.getUserFriendlyQuery(notEmptyCondition, entityType);
    assertEquals(expectedNotEmptyCondition, actualRegexCondition);
  }

  @Test
  void shouldGetStringForFqlAndCondition() {
    EntityType entityType = new EntityType();
    RegexCondition regexCondition = new RegexCondition(new FqlField("field1"), "^some value");
    LessThanCondition lessThanCondition = new LessThanCondition(new FqlField("field1"), false, "some value");
    AndCondition andCondition = new AndCondition(List.of(regexCondition, lessThanCondition));
    String expectedAndCondition = "(field1 starts with some value) AND (field1 < some value)";
    String actualAndCondition = userFriendlyQueryService.getUserFriendlyQuery(andCondition, entityType);
    assertEquals(expectedAndCondition, actualAndCondition);
  }

  @Test
  void shouldProperlySerializeNestedProperties() {
    EntityType entityType = new EntityType();
    EmptyCondition condition = new EmptyCondition(new FqlField("field1[*]->foo->bar->baz"), false);
    String expectedCondition = "field1[*]->foo->bar->baz is not empty";
    String actualRegexCondition = userFriendlyQueryService.getUserFriendlyQuery(condition, entityType);
    assertEquals(expectedCondition, actualRegexCondition);
  }
}
