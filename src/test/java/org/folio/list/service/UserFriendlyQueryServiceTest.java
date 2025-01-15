package org.folio.list.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.folio.fql.model.AndCondition;
import org.folio.fql.model.ContainsAllCondition;
import org.folio.fql.model.ContainsAnyCondition;
import org.folio.fql.model.EmptyCondition;
import org.folio.fql.model.EqualsCondition;
import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
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
import org.folio.list.rest.ConfigurationClient;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.rest.QueryClient;
import org.folio.list.services.ListActions;
import org.folio.list.services.UserFriendlyQueryService;
import org.folio.querytool.domain.dto.DateType;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.querytool.domain.dto.StringType;
import org.folio.querytool.domain.dto.ValueWithLabel;
import org.folio.spring.i18n.service.TranslationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    EntityType entityType = new EntityType()
      .columns(List.of(new EntityTypeColumn().name("field1").dataType(new StringType())));
    EqualsCondition equalsCondition = new EqualsCondition(new FqlField("field1"), "some value");
    String expectedEqualsCondition = "field1 == some value";

    when(fqlService.getFql("query")).thenReturn(new Fql("", equalsCondition));

    String actualEqualsConditions = userFriendlyQueryService.getUserFriendlyQuery("query", entityType);
    assertEquals(expectedEqualsCondition, actualEqualsConditions);
  }

  @Test
  void testUpdateWithProvidedEntityType() {
    EntityType entityType = new EntityType()
      .columns(List.of(new EntityTypeColumn().name("field1").dataType(new StringType())));
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
    List<EntityTypeColumn> columns = List.of(new EntityTypeColumn().name("field1").dataType(new StringType()));

    when(fqlService.getFql("query")).thenReturn(new Fql("", equalsCondition));
    when(entityTypeClient.getEntityType(testList.getEntityTypeId(), ListActions.UPDATE, true))
      .thenReturn(new EntityType().columns(columns));

    userFriendlyQueryService.updateListUserFriendlyQuery(testList);
    assertEquals(expectedEqualsCondition, testList.getUserFriendlyQuery());
  }

  static List<Arguments> basicConditionCases() {
    // fieldStr is provided as a string column
    return List.of(
      Arguments.of(new InCondition(new FqlField("field1"), List.of("value1", "value2")), "field1 in [value1, value2]"),
      Arguments.of(
        new NotInCondition(new FqlField("field1"), List.of("value1", "value2")),
        "field1 not in [value1, value2]"
      ),
      Arguments.of(new ContainsAllCondition(new FqlField("field1"), List.of("value1")), "field1 contains all [value1]"),
      Arguments.of(new ContainsAnyCondition(new FqlField("field1"), List.of("value1")), "field1 contains any [value1]"),
      Arguments.of(
        new NotContainsAllCondition(new FqlField("field1"), List.of("value1")),
        "field1 does not contain all [value1]"
      ),
      Arguments.of(
        new NotContainsAnyCondition(new FqlField("field1"), List.of("value1")),
        "field1 does not contain any [value1]"
      ),
      Arguments.of(new RegexCondition(new FqlField("field1"), "^some value"), "field1 starts with some value"),
      Arguments.of(new RegexCondition(new FqlField("field1"), "some value"), "field1 contains some value"),
      Arguments.of(new EmptyCondition(new FqlField("field1"), true), "field1 is empty"),
      Arguments.of(new EmptyCondition(new FqlField("field1"), false), "field1 is not empty"),
      Arguments.of(
        new EmptyCondition(new FqlField("field1[*]->foo->bar->baz"), false),
        "field1[*]->foo->bar->baz is not empty"
      ),
      Arguments.of(new EqualsCondition(new FqlField("field1"), "some value"), "field1 == some value"),
      Arguments.of(new NotEqualsCondition(new FqlField("field1"), "some value"), "field1 != some value"),
      Arguments.of(new GreaterThanCondition(new FqlField("field1"), false, "some value"), "field1 > some value"),
      Arguments.of(new GreaterThanCondition(new FqlField("field1"), true, "some value"), "field1 >= some value"),
      Arguments.of(new LessThanCondition(new FqlField("field1"), false, "some value"), "field1 < some value"),
      Arguments.of(new LessThanCondition(new FqlField("field1"), true, "some value"), "field1 <= some value"),
      Arguments.of(
        new AndCondition(
          List.of(
            new RegexCondition(new FqlField("field1"), "^some value"),
            new LessThanCondition(new FqlField("field1"), false, "some value")
          )
        ),
        "(field1 starts with some value) AND (field1 < some value)"
      ),
      Arguments.of(new EqualsCondition(new FqlField("customField"), "value1"), "customField == label1"),
      Arguments.of(new InCondition(new FqlField("customField"), List.of("value1", "value2")), "customField in [label1, label2]")
    );
  }

  @ParameterizedTest(name = "basic condition {0} gives {1}")
  @MethodSource("basicConditionCases")
  void testBasicConditionGeneration(FqlCondition<Object> condition, String expected) {
    EntityType entityType = new EntityType()
      .columns(
        List.of(
          new EntityTypeColumn()
            .name("field1")
            .dataType(new StringType().dataType("stringType")),
          new EntityTypeColumn()
            .name("customField")
            .isCustomField(true)
            .dataType(new StringType().dataType("stringType"))
            .values(
              List.of(
                new ValueWithLabel().value("value1").label("label1"),
                new ValueWithLabel().value("value2").label("label2")
              )
            )
        )
      );

    String actual = userFriendlyQueryService.getUserFriendlyQuery(condition, entityType);
    assertEquals(expected, actual);
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
  void testThrowsExceptionForInvalidDateFormat() {
    EntityTypeColumn column = new EntityTypeColumn().name("field1").dataType(new DateType().dataType("dateType"));
    EntityType entityType = new EntityType().columns(List.of(column));
    GreaterThanCondition greaterThanCondition = new GreaterThanCondition(new FqlField("field1"), true, "2024-10-01T23:00:00");
    assertThrows(
      IllegalArgumentException.class,
      () -> userFriendlyQueryService.getUserFriendlyQuery(greaterThanCondition, entityType)
    );
  }
}
