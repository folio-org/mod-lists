package org.folio.list.service;

import org.folio.fql.model.AndCondition;
import org.folio.fql.model.ContainsAllCondition;
import org.folio.fql.model.ContainsAnyCondition;
import org.folio.fql.model.EmptyCondition;
import org.folio.fql.model.EqualsCondition;
import org.folio.fql.model.Fql;
import org.folio.fql.model.GreaterThanCondition;
import org.folio.fql.model.InCondition;
import org.folio.fql.model.LessThanCondition;
import org.folio.fql.model.NotContainsAllCondition;
import org.folio.fql.model.NotContainsAnyCondition;
import org.folio.fql.model.NotEqualsCondition;
import org.folio.fql.model.NotInCondition;
import org.folio.fql.model.RegexCondition;
import org.folio.fql.model.field.FqlField;
import org.folio.fql.service.FqlService;
import org.folio.list.domain.ListEntity;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.rest.ConfigurationClient;
import org.folio.list.rest.QueryClient;
import org.folio.list.services.ListActions;
import org.folio.list.services.UserFriendlyQueryService;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.querytool.domain.dto.DateType;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.querytool.domain.dto.RangedUUIDType;
import org.folio.querytool.domain.dto.SourceColumn;
import org.folio.querytool.domain.dto.StringType;
import org.folio.spring.i18n.service.TranslationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFriendlyQueryServiceTest {

  @InjectMocks
  private UserFriendlyQueryService userFriendlyQueryService;

  @Mock
  private EntityTypeClient entityTypeClient;

  @Mock
  private FqlService fqlService;

  @Mock
  private QueryClient queryClient;

  @Mock
  private ConfigurationClient configurationClient;

  @Mock
  private TranslationService translationService;

  @Test
  void testGetAndDeserialize() {
    List<EntityTypeColumn> columns = List.of(
      new EntityTypeColumn().name("field1").dataType(new StringType())
    );
    EntityType entityType = new EntityType().columns(columns);
    EqualsCondition equalsCondition = new EqualsCondition(new FqlField("field1"), "some value");
    String expectedEqualsCondition = "field1 == some value";

    when(fqlService.getFql("query")).thenReturn(new Fql("", equalsCondition));

    String actualEqualsConditions = userFriendlyQueryService.getUserFriendlyQuery("query", entityType);
    assertEquals(expectedEqualsCondition, actualEqualsConditions);
  }

  @Test
  void testUpdateWithProvidedEntityType() {
    List<EntityTypeColumn> columns = List.of(
      new EntityTypeColumn().name("field1").dataType(new StringType())
    );
    EntityType entityType = new EntityType().columns(columns);
    EqualsCondition equalsCondition = new EqualsCondition(new FqlField("field1"), "some value");
    String expectedEqualsCondition = "field1 == some value";
    ListEntity testList = new ListEntity().withFqlQuery("query");

    when(fqlService.getFql("query")).thenReturn(new Fql("", equalsCondition));

    userFriendlyQueryService.updateListUserFriendlyQuery(testList, entityType);
    assertEquals(expectedEqualsCondition, testList.getUserFriendlyQuery());

    verifyNoInteractions(entityTypeClient);
  }

  @Test
  void testUpdateWithoutProvidedEntityType() {
    EqualsCondition equalsCondition = new EqualsCondition(new FqlField("field1"), "some value");
    String expectedEqualsCondition = "field1 == some value";
    ListEntity testList = new ListEntity()
      .withEntityTypeId(UUID.fromString("39bf039d-a582-5758-878c-185aeb88e679"))
      .withFqlQuery("query");
    List<EntityTypeColumn> columns = List.of(
      new EntityTypeColumn().name("field1").dataType(new StringType())
    );

    when(fqlService.getFql("query")).thenReturn(new Fql("", equalsCondition));
    when(entityTypeClient.getEntityType(testList.getEntityTypeId(), ListActions.UPDATE, true)).thenReturn(new EntityType().columns(columns));

    userFriendlyQueryService.updateListUserFriendlyQuery(testList);
    assertEquals(expectedEqualsCondition, testList.getUserFriendlyQuery());
  }

  @Test
  void shouldGetStringForFqlEqualsConditionWithoutIdColumn() {
    EntityTypeColumn column = new EntityTypeColumn().name("field1").dataType(new StringType().dataType("stringType"));
    EntityType entityType = new EntityType().columns(List.of(column));
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
        new EntityTypeColumn().name("underlying")
          .dataType(new RangedUUIDType().dataType("rangedUUIDType")),
        new EntityTypeColumn().name("friendly")
          .dataType(new StringType().dataType("stringType"))
          .idColumnName("underlying")
          .source(new SourceColumn().entityTypeId(sourceEntityTypeId).columnName("sourceField"))
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
        new EntityTypeColumn().name("underlying")
          .dataType(new RangedUUIDType().dataType("rangedUUIDType")),
        new EntityTypeColumn().name("friendly")
          .dataType(new StringType().dataType("stringType"))
          .idColumnName("underlying")
          .source(new SourceColumn().entityTypeId(sourceEntityTypeId).columnName("sourceField"))
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
    EntityTypeColumn column = new EntityTypeColumn().name("field1").dataType(new StringType().dataType("stringType"));
    EntityType entityType = new EntityType().columns(List.of(column));
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
    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId).columnName("field1");
    EntityTypeColumn column = new EntityTypeColumn().name("field1").dataType(new RangedUUIDType().dataType("rangedUUIDType"));
    EntityTypeColumn column1 = new EntityTypeColumn().name("field2").idColumnName("field1").source(sourceColumn).dataType(new StringType().dataType("stringType"));
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
    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId).columnName("field1");
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
    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId).columnName("field1");
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
    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId).columnName("field1");
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
    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId).columnName("field1");
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
    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId).columnName("field1");
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
    SourceColumn sourceColumn = new SourceColumn().entityTypeId(sourceEntityTypeId).columnName("field1");
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
    EntityTypeColumn column = new EntityTypeColumn().name("field1").dataType(new StringType().dataType("stringType"));
    EntityType entityType = new EntityType().columns(List.of(column));
    GreaterThanCondition greaterThanCondition = new GreaterThanCondition(new FqlField("field1"), false, "some value");
    String expectedGreaterThanCondition = "field1 > some value";
    String actualGreaterThanCondition = userFriendlyQueryService.getUserFriendlyQuery(greaterThanCondition, entityType);
    assertEquals(expectedGreaterThanCondition, actualGreaterThanCondition);
  }

  @Test
  void shouldGetStringForFqlGreaterThanEqualToCondition() {
    EntityTypeColumn column = new EntityTypeColumn().name("field1").dataType(new StringType().dataType("stringType"));
    EntityType entityType = new EntityType().columns(List.of(column));
    GreaterThanCondition greaterThanEqualCondition = new GreaterThanCondition(new FqlField("field1"), true, "some value");
    String expectedGreaterThanEqualCondition = "field1 >= some value";
    String actualGreaterThanEqualCondition = userFriendlyQueryService.getUserFriendlyQuery(greaterThanEqualCondition, entityType);
    assertEquals(expectedGreaterThanEqualCondition, actualGreaterThanEqualCondition);
  }

  @Test
  void shouldGetStringForFqlLessThanCondition() {
    EntityTypeColumn column = new EntityTypeColumn().name("field1").dataType(new StringType().dataType("stringType"));
    EntityType entityType = new EntityType().columns(List.of(column));
    LessThanCondition lessThanCondition = new LessThanCondition(new FqlField("field1"), false, "some value");
    String expectedLessThanCondition = "field1 < some value";
    String actualLessThanCondition = userFriendlyQueryService.getUserFriendlyQuery(lessThanCondition, entityType);
    assertEquals(expectedLessThanCondition, actualLessThanCondition);
  }

  @Test
  void shouldGetStringForFqlLessThanEqualToCondition() {
    EntityTypeColumn column = new EntityTypeColumn().name("field1").dataType(new StringType().dataType("stringType"));
    EntityType entityType = new EntityType().columns(List.of(column));
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
    EntityTypeColumn column = new EntityTypeColumn().name("field1").dataType(new StringType().dataType("stringType"));
    EntityType entityType = new EntityType().columns(List.of(column));
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

  static List<Arguments> dateConditionCases() {
    return List.of(
      // zone, input, expected timestamp
      // (will be printed in m/d/yy in normal usage; we expose verbose expected here to ensure we get the right time)

      // date only + during summer/daylight savings
      Arguments.of(ZoneId.of("UTC"), "2024-09-01", "2024-09-01T00:00Z[UTC]"),
      Arguments.of(ZoneId.of("America/New_York"), "2024-09-01", "2024-09-01T00:00-04:00[America/New_York]"),
      Arguments.of(ZoneId.of("Australia/Adelaide"), "2024-09-01", "2024-09-01T00:00+09:30[Australia/Adelaide]"),

      // date only + non-summer time
      Arguments.of(ZoneId.of("UTC"), "2024-02-01", "2024-02-01T00:00Z[UTC]"),
      Arguments.of(ZoneId.of("America/New_York"), "2024-02-01", "2024-02-01T00:00-05:00[America/New_York]"),
      Arguments.of(ZoneId.of("Australia/Adelaide"), "2024-02-01", "2024-02-01T00:00+10:30[Australia/Adelaide]"),

      // full timestamp + during summer/daylight savings
      Arguments.of(ZoneId.of("UTC"), "2024-09-01T00:00:00.000", "2024-09-01T00:00Z[UTC]"),
      Arguments.of(ZoneId.of("UTC"), "2024-09-01T00:00:00.000Z", "2024-09-01T00:00Z[UTC]"),
      Arguments.of(ZoneId.of("America/New_York"), "2024-09-01T04:00:00.000", "2024-09-01T00:00-04:00[America/New_York]"),
      Arguments.of(ZoneId.of("America/New_York"), "2024-09-01T04:00:00.000Z", "2024-09-01T00:00-04:00[America/New_York]"),
      Arguments.of(ZoneId.of("Australia/Adelaide"), "2024-08-31T14:30:00.000", "2024-09-01T00:00+09:30[Australia/Adelaide]"),

      // full timestamp + non-summer time
      Arguments.of(ZoneId.of("UTC"), "2024-02-01T00:00:00.000", "2024-02-01T00:00Z[UTC]"),
      Arguments.of(ZoneId.of("UTC"), "2024-02-01T00:00:00.000Z", "2024-02-01T00:00Z[UTC]"),
      Arguments.of(ZoneId.of("America/New_York"), "2024-02-01T05:00:00.000", "2024-02-01T00:00-05:00[America/New_York]"),
      Arguments.of(ZoneId.of("America/New_York"), "2024-02-01T05:00:00.000Z", "2024-02-01T00:00-05:00[America/New_York]"),
      Arguments.of(ZoneId.of("Australia/Adelaide"), "2024-01-31T13:30:00.000", "2024-02-01T00:00+10:30[Australia/Adelaide]"),

      // and, why not, some non-midnight times (we won't display them, but we might support these in the future?)
      Arguments.of(ZoneId.of("UTC"), "2024-02-01T12:30:00.000", "2024-02-01T12:30Z[UTC]"),
      Arguments.of(ZoneId.of("America/New_York"), "2024-02-01T09:00:00.000", "2024-02-01T04:00-05:00[America/New_York]"),
      Arguments.of(ZoneId.of("Australia/Adelaide"), "2024-02-01T12:00:00.000", "2024-02-01T22:30+10:30[Australia/Adelaide]")
    );
  }

  @ParameterizedTest(name = "date conversion for tz {0} input {1} gives {2}")
  @MethodSource("dateConditionCases")
  void testDateConditionLocalization(ZoneId zone, String input, String expected) {
    EntityTypeColumn column = new EntityTypeColumn().name("field1").dataType(new DateType().dataType("dateType"));
    EntityType entityType = new EntityType().columns(List.of(column));
    EqualsCondition equalsCondition = new EqualsCondition(new FqlField("field1"), input);

    when(configurationClient.getTenantTimezone()).thenReturn(zone);
    when(translationService.formatString(any(), any(String.class), any(Object[].class)))
      .thenAnswer(iv -> {
        // the actual conversion is handled by the translation service, so we just need to check the arguments
        assertEquals((ZoneId) iv.getArgument(0), zone);

        // verbose version from ZonedDateTime, to ensure we get midnight
        return ((Instant) iv.getArgument(3)).atZone(zone).toString();
      });

    String expectedQuery = "field1 == " + expected;
    String actualQuery = userFriendlyQueryService.getUserFriendlyQuery(equalsCondition, entityType);
    assertEquals(expectedQuery, actualQuery);
  }

  @Test
  void shouldThrowExceptionForInvalidDateFormat() {
    EntityTypeColumn column = new EntityTypeColumn().name("field1").dataType(new DateType().dataType("dateType"));
    EntityType entityType = new EntityType().columns(List.of(column));
    EqualsCondition equalsCondition = new EqualsCondition(new FqlField("field1"), "2024-10-01T23:00:00");
    assertThrows(IllegalArgumentException.class, () -> userFriendlyQueryService.getUserFriendlyQuery(equalsCondition, entityType));
  }
}
