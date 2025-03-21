package org.folio.list.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.fql.model.ContainsAllCondition;
import org.folio.fql.model.ContainsAnyCondition;
import org.folio.fql.model.EqualsCondition;
import org.folio.fql.model.FieldCondition;
import org.folio.fql.model.InCondition;
import org.folio.fql.model.NotContainsAllCondition;
import org.folio.fql.model.NotContainsAnyCondition;
import org.folio.fql.model.NotEqualsCondition;
import org.folio.fql.model.NotInCondition;
import org.folio.fql.model.field.FqlField;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.services.UserFriendlyQueryService;
import org.folio.querytool.domain.dto.ColumnValues;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.querytool.domain.dto.RangedUUIDType;
import org.folio.querytool.domain.dto.SourceColumn;
import org.folio.querytool.domain.dto.StringType;
import org.folio.querytool.domain.dto.ValueWithLabel;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserFriendlyQueryServiceIdColumnTest {

  @InjectMocks
  private UserFriendlyQueryService userFriendlyQueryService;

  @Mock
  private EntityTypeClient entityTypeClient;

  static final UUID ENTITY_TYPE_ID = UUID.fromString("7ad9f9de-40dc-5d88-a49e-515adb4b470c");

  static final UUID VALUE_ID1 = UUID.fromString("165c959a-0838-55e1-b878-d0f9476dc582");
  static final UUID VALUE_ID2 = UUID.fromString("d901ca34-c1db-58ec-8942-6d9335675899");
  static final String VALUE_RESOLVED1 = "value1";
  static final String VALUE_RESOLVED2 = "value2";

  static List<Arguments> idColumnTestCases() {
    return Stream
      // both of these should map to underlying for values, and render as friendly
      .of("underlying", "friendly")
      .flatMap(field ->
        Stream.of(
          Arguments.of(new EqualsCondition(new FqlField(field), VALUE_ID1), "friendly == value1"),
          Arguments.of(new NotEqualsCondition(new FqlField(field), VALUE_ID1), "friendly != value1"),
          Arguments.of(
            new InCondition(new FqlField(field), List.of(VALUE_ID1, VALUE_ID2)),
            "friendly in [value1, value2]"
          ),
          Arguments.of(
            new NotInCondition(new FqlField(field), List.of(VALUE_ID1, VALUE_ID2)),
            "friendly not in [value1, value2]"
          ),
          Arguments.of(
            new ContainsAllCondition(new FqlField(field), List.of(VALUE_ID1, VALUE_ID2)),
            "friendly contains all [value1, value2]"
          ),
          Arguments.of(
            new NotContainsAllCondition(new FqlField(field), List.of(VALUE_ID1, VALUE_ID2)),
            "friendly does not contain all [value1, value2]"
          ),
          Arguments.of(
            new ContainsAnyCondition(new FqlField(field), List.of(VALUE_ID1, VALUE_ID2)),
            "friendly contains any [value1, value2]"
          ),
          Arguments.of(
            new NotContainsAnyCondition(new FqlField(field), List.of(VALUE_ID1, VALUE_ID2)),
            "friendly does not contain any [value1, value2]"
          )
        )
      )
      .toList();
  }

  @ParameterizedTest(name = "condition {0} gives {1}")
  @MethodSource("idColumnTestCases")
  void testIdColumnQueryValueMapping(FieldCondition<UUID> condition, String expected) {
    Map<String, String> valuesAndLabels = Map.of(VALUE_ID1.toString(), VALUE_RESOLVED1, VALUE_ID2.toString(), VALUE_RESOLVED2);

    EntityType entityType = new EntityType()
      .id(ENTITY_TYPE_ID.toString())
      .columns(
        List.of(
          new EntityTypeColumn().name("underlying").dataType(new RangedUUIDType().dataType("rangedUUIDType")),
          new EntityTypeColumn()
            .name("friendly")
            .idColumnName("underlying")
            .source(new SourceColumn().entityTypeId(ENTITY_TYPE_ID).columnName("underlying"))
            .dataType(new StringType().dataType("stringType"))
        )
      );

    lenient()
      .when(entityTypeClient.getColumnValues(eq(ENTITY_TYPE_ID), eq("underlying")))
      .thenReturn(new ColumnValues(mapToValueWithLabel(valuesAndLabels)));

    String actual = userFriendlyQueryService.getUserFriendlyQuery(condition, entityType);
    assertEquals(expected, actual);

    verify(entityTypeClient, times(1)).getColumnValues(any(), any());
  }

  private static List<ValueWithLabel> mapToValueWithLabel(Map<String, String> valuesAndLabels) {
    return valuesAndLabels.entrySet().stream()
      .map(e -> new ValueWithLabel(e.getKey()).label(e.getValue()))
      .toList();
  }
}
