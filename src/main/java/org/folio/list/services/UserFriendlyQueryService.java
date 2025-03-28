package org.folio.list.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.fql.model.*;
import org.folio.fql.service.FqlService;
import org.folio.fql.service.FqlValidationService;
import org.folio.list.domain.ListEntity;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.rest.ConfigurationClient;
import org.folio.querytool.domain.dto.DateType;
import org.folio.querytool.domain.dto.EntityDataType;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.querytool.domain.dto.Field;
import org.folio.querytool.domain.dto.ValueWithLabel;
import org.folio.spring.i18n.service.TranslationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserFriendlyQueryService {

  private final EntityTypeClient entityTypeClient;
  private final FqlService fqlService;
  private final TranslationService translationService;

  private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
    .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    .optionalStart().appendOffsetId() // optional Z/tz at end
    .toFormatter().withZone(ZoneOffset.UTC); // force interpretation as UTC
  private static final String DATE_REGEX = "^\\d{4}-\\d{2}-\\d{2}$";
  private static final String DATE_TIME_REGEX = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z?$";
  private static final String INVALID_DATE_STRING = "Date %s is not valid. Dates should match format 'yyyy-MM-dd' or 'yyyy-MM-dd'T'HH:mm:ss.SSS'";

  private final ConfigurationClient configurationClient;

  private final Map<Class<? extends FqlCondition<?>>, BiFunction<FqlCondition<?>, EntityType, String>> userFriendlyQuery = Map.ofEntries(
    Map.entry(EqualsCondition.class, (cnd, ent) -> handleEquals((EqualsCondition) cnd, ent)),
    Map.entry(NotEqualsCondition.class, (cnd, ent) -> handleNotEquals((NotEqualsCondition) cnd, ent)),
    Map.entry(InCondition.class, (cnd, ent) -> handleIn((InCondition) cnd, ent)),
    Map.entry(NotInCondition.class, (cnd, ent) -> handleNotIn((NotInCondition) cnd, ent)),
    Map.entry(GreaterThanCondition.class, (cnd, ent) -> handleGreaterThan((GreaterThanCondition) cnd, ent)),
    Map.entry(LessThanCondition.class, (cnd, ent) -> handleLessThan((LessThanCondition) cnd, ent)),
    Map.entry(AndCondition.class, (cnd, ent) -> handleAnd((AndCondition) cnd, ent)),
    Map.entry(RegexCondition.class, (cnd, ent) -> handleRegEx((RegexCondition) cnd)),
    Map.entry(ContainsAllCondition.class, (cnd, ent) -> handleContainsAll((ContainsAllCondition) cnd, ent)),
    Map.entry(NotContainsAllCondition.class, (cnd, ent) -> handleNotContainsAll((NotContainsAllCondition) cnd, ent)),
    Map.entry(ContainsAnyCondition.class, (cnd, ent) -> handleContainsAny((ContainsAnyCondition) cnd, ent)),
    Map.entry(ContainsCondition.class, (cnd, ent) -> handleContains((ContainsCondition) cnd, ent)),
    Map.entry(StartsWithCondition.class, (cnd, ent) -> handleStartsWith((StartsWithCondition) cnd, ent)),
    Map.entry(NotContainsAnyCondition.class, (cnd, ent) -> handleNotContainsAny((NotContainsAnyCondition) cnd, ent)),
    Map.entry(EmptyCondition.class, (cnd, ent) -> handleEmpty((EmptyCondition) cnd))
  );

  public String getUserFriendlyQuery(FqlCondition<?> fqlCondition, EntityType entityType) {
    log.info("Computing user friendly query for fqlCondition: {}, entityType: {}", fqlCondition, entityType.getId());
    return userFriendlyQuery.get(fqlCondition.getClass()).apply(fqlCondition, entityType);
  }

  public String getUserFriendlyQuery(String fqlCriteria, EntityType entityType) {
    Fql fql = fqlService.getFql(fqlCriteria);
    if (fql.fqlCondition() == null) {
      return "";
    }
    return getUserFriendlyQuery(fql.fqlCondition(), entityType);
  }

  public void updateListUserFriendlyQuery(ListEntity listEntity) {
    listEntity.setUserFriendlyQuery(
      getUserFriendlyQuery(listEntity.getFqlQuery(), entityTypeClient.getEntityType(listEntity.getEntityTypeId(), ListActions.UPDATE, true))
    );
  }

  public void updateListUserFriendlyQuery(ListEntity listEntity, EntityType entityType) {
    listEntity.setUserFriendlyQuery(
      getUserFriendlyQuery(listEntity.getFqlQuery(), entityType)
    );
  }

  private String handleGreaterThan(GreaterThanCondition greaterThanCondition, EntityType entityType) {
    String operator = greaterThanCondition.orEqualTo() ? " >= " : " > ";
    return getColumnName(greaterThanCondition, entityType) + operator + getConditionValue(greaterThanCondition, entityType);
  }

  private String handleLessThan(LessThanCondition lessThanCondition, EntityType entityType) {
    String operator = lessThanCondition.orEqualTo() ? " <= " : " < ";
    return getColumnName(lessThanCondition, entityType) + operator + getConditionValue(lessThanCondition, entityType);
  }

  private String handleAnd(AndCondition andCondition, EntityType entityType) {
    return andCondition.value()
      .stream()
      .map(cnd -> this.getUserFriendlyQuery(cnd, entityType))
      .map(s -> '(' + s + ')')
      .collect(Collectors.joining(" AND "));
  }

  private String handleContains(ContainsCondition containsCondition, EntityType entityType) {
    return getColumnName(containsCondition, entityType) + " contains " + containsCondition.value();
  }

  private String handleStartsWith(StartsWithCondition startsWithCondition, EntityType entityType) {
    return getColumnName(startsWithCondition, entityType) + " starts with  " + startsWithCondition.value();
  }

  private String handleRegEx(RegexCondition regExCondition) {
    if (regExCondition.value().startsWith("^")) {
      return regExCondition.field().serialize() + " starts with " + regExCondition.value().substring(1);
    }
    return regExCondition.field().serialize() + " contains " + regExCondition.value();
  }

  private String handleEquals(EqualsCondition equalsCondition, EntityType entityType) {
    BiFunction<Field, Object, String> labelFn = (col, val) -> this.getLabel(UUID.fromString(val.toString()), col);
    return handleConditionWithPossibleIdValue(equalsCondition, getColumnName(equalsCondition, entityType), entityType, "==", labelFn);
  }

  private String handleNotEquals(NotEqualsCondition notEqualsCondition, EntityType entityType) {
    BiFunction<Field, Object, String> labelFn = (col, val) -> this.getLabel(UUID.fromString(val.toString()), col);
    return handleConditionWithPossibleIdValue(notEqualsCondition, getColumnName(notEqualsCondition, entityType), entityType, "!=", labelFn);
  }

  private String handleIn(InCondition inCondition, EntityType entityType) {
    BiFunction<Field, List<Object>, String> labelFn = (col, val) -> {
      List<String> ids = val.stream().map(Object::toString).toList();
      return getLabel(ids, col, true);
    };
    return handleConditionWithPossibleIdValue(inCondition, getColumnName(inCondition, entityType), entityType, "in", labelFn);
  }

  private String handleNotIn(NotInCondition notInCondition, EntityType entityType) {
    BiFunction<Field, List<Object>, String> labelFn = (col, val) -> {
      List<String> ids = val.stream().map(Object::toString).toList();
      return getLabel(ids, col, true);
    };
    return handleConditionWithPossibleIdValue(notInCondition, getColumnName(notInCondition, entityType), entityType, "not in", labelFn);
  }

  private String handleContainsAll(ContainsAllCondition containsAllCondition, EntityType entityType) {
    BiFunction<Field, List<Object>, String> labelFn = (col, val) -> {
      List<String> ids = val.stream().map(Object::toString).toList();
      return getLabel(ids, col, true);
    };
    return handleConditionWithPossibleIdValue(containsAllCondition, getColumnName(containsAllCondition, entityType), entityType, "contains all", labelFn);
  }

  private String handleNotContainsAll(NotContainsAllCondition notContainsAllCondition, EntityType entityType) {
    BiFunction<Field, List<Object>, String> labelFn = (col, val) -> {
      List<String> ids = val.stream().map(Object::toString).toList();
      return getLabel(ids, col, true);
    };
    return handleConditionWithPossibleIdValue(notContainsAllCondition, getColumnName(notContainsAllCondition, entityType), entityType, "does not contain all", labelFn);
  }

  private String handleContainsAny(ContainsAnyCondition containsAnyCondition, EntityType entityType) {
    BiFunction<Field, List<Object>, String> labelFn = (col, val) -> {
      List<String> ids = val.stream().map(Object::toString).toList();
      return getLabel(ids, col, true);
    };
    return handleConditionWithPossibleIdValue(containsAnyCondition, getColumnName(containsAnyCondition, entityType), entityType, "contains any", labelFn);
  }

  private String handleNotContainsAny(NotContainsAnyCondition notContainsAnyCondition, EntityType entityType) {
    BiFunction<Field, List<Object>, String> labelFn = (col, val) -> {
      List<String> ids = val.stream().map(Object::toString).toList();
      return getLabel(ids, col, true);
    };
    return handleConditionWithPossibleIdValue(notContainsAnyCondition, getColumnName(notContainsAnyCondition, entityType), entityType, "does not contain any", labelFn);
  }

  private String handleEmpty(EmptyCondition emptyCondition) {
    if (Boolean.TRUE.equals(emptyCondition.value())) {
      return emptyCondition.field().serialize() + " is empty";
    } else {
      return emptyCondition.field().serialize() + " is not empty";
    }
  }

  private Object getConditionValue(FieldCondition<?> fieldCondition, EntityType entityType) {
    String columnName = fieldCondition.field().getColumnName();
    EntityTypeColumn column = entityType
      .getColumns()
      .stream()
      .filter(col -> col.getName().equals(columnName))
      .findFirst()
      .orElseThrow();
    EntityDataType dataType = column.getDataType();
    boolean isCustomField = Boolean.TRUE.equals(column.getIsCustomField());
    if (isCustomField) {
      return getCustomFieldValue(fieldCondition, column);
    }

    if (!(dataType instanceof DateType)) {
      return fieldCondition.value();
    }

    ZoneId tenantTimezone = configurationClient.getTenantTimezone();

    return translationService.formatString(
      tenantTimezone,
      "{value, date, short}", "value",
      handleDateValue((String) fieldCondition.value(), tenantTimezone)
    );
  }

  private Object getCustomFieldValue(FieldCondition<?> fieldCondition, EntityTypeColumn column) {
    Object value = fieldCondition.value();
    if (value instanceof List<?> valueList) {
      return valueList
        .stream()
        .map(val -> findMatchingLabel(val, column))
        .filter(Objects::nonNull)
        .toList();
    }
    return findMatchingLabel(value, column);
  }

  private Object findMatchingLabel(Object value, EntityTypeColumn column) {
    return column
      .getValues()
      .stream()
      .filter(col -> col.getValue().equals(value))
      .findFirst()
      .orElse(new ValueWithLabel())
      .getLabel();
  }

  private Instant handleDateValue(String value, ZoneId timezone) {
    String dateString = value;
    if (value.matches(DATE_REGEX)) {
      return LocalDate.parse(value).atStartOfDay(timezone).toInstant();
    } else if (value.matches(DATE_TIME_REGEX)) {
      return Instant.from(DATE_TIME_FORMATTER.parse(value));
    } else {
      // We should never end up here because mod-fqm-manager validates queries first, but just in case
      throw new IllegalArgumentException(INVALID_DATE_STRING.formatted(dateString));
    }
  }

  private <T> String handleConditionWithPossibleIdValue(FieldCondition<T> condition,
                                                        String columnName,
                                                        EntityType entityType,
                                                        String userFriendlyOperator,
                                                        BiFunction<Field, T, String> labelFn) {
    T value = condition.value();
    String operatorWithPadding = " " + userFriendlyOperator + " ";
    try {
      // the field referenced directly in the query. May or may not have a source or ID column
      // e.g. user patron group, vendor code, vendor ID, etc
      Field querySubjectField = FqlValidationService.findFieldDefinition(condition.field(), entityType).orElseThrow();
      if (querySubjectField.getIdColumnName() != null) {
        return columnName + operatorWithPadding + labelFn.apply(querySubjectField, value);
      } else {
        // a column which uses our query subject as ID
        // this will be found when querySubjectField is something like vendor_id, etc.; we would find something like vendor_code
        Optional<Field> referencingField = entityType.getColumns()
          .stream()
          // finds the column referencing this
          .filter(column -> column.getSource() != null && condition.field().getColumnName().equals(column.getIdColumnName()))
          .findFirst()
          .map(Field.class::cast);

        return referencingField
          // if we found this referencing column, use it as the basis for getting the label
          .map(column -> column.getName() + operatorWithPadding + labelFn.apply(column, condition.value()))
          // sensible fallback
          .orElse(columnName + operatorWithPadding + getConditionValue(condition, entityType));
      }
    } catch (Exception e) {
      log.error("Unexpected error when creating user friendly query for condition " + condition + ". Exception: " + e);
      return columnName + operatorWithPadding + condition.value();
    }
  }

  private String getColumnName(FieldCondition<?> condition, EntityType entityType) {
    EntityTypeColumn currentColumn = entityType.getColumns()
      .stream()
      .filter(column -> condition.field().getColumnName().equals(column.getName()))
      .findFirst()
      .orElseThrow();
    if (Boolean.TRUE.equals(currentColumn.getIsCustomField())) {
      return currentColumn.getLabelAlias();
    }
    return condition.field().serialize();
  }

  private String getLabel(UUID id, Field column) {
    return getLabel(List.of(id.toString()), column, false);
  }

  private String getLabel(List<String> ids, Field field, boolean addBrackets) {
    var collector = Boolean.TRUE.equals(addBrackets) ? Collectors.joining(", ", "[", "]") :
      Collectors.joining(",");

    UUID sourceEntityTypeId = field.getSource().getEntityTypeId();
    String valueColumnName = field.getSource().getColumnName();
    log.info(
      "Getting label for ids {} on field {} (derived to {} in entity type {})",
      ids,
      field.getName(),
      valueColumnName,
      sourceEntityTypeId
    );

    var valuesAandLabels = entityTypeClient.getColumnValues(sourceEntityTypeId, field.getSource().getColumnName())
      .getContent()
      .stream()
      .collect(Collectors.toMap(ValueWithLabel::getValue, ValueWithLabel::getLabel, (a, b) -> a));

    return ids.stream()
      .map(id -> valuesAandLabels.getOrDefault(id, "?"))
      .collect(collector);
  }
}
