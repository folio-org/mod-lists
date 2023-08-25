package org.folio.list.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.fql.model.*;
import org.folio.fqm.lib.service.ResultSetService;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


@Log4j2
@Service
@RequiredArgsConstructor
public class UserFriendlyQueryService {
  private final ResultSetService resultSetService;
  private final FolioExecutionContext executionContext;
  private final Map<Class<? extends FqlCondition<?>>, BiFunction<FqlCondition<?>, EntityType, String>> userFriendlyQuery = Map.of(
    EqualsCondition.class, (cnd, ent) -> handleEquals((EqualsCondition) cnd, ent),
    NotEqualsCondition.class, (cnd, ent) -> handleNotEquals((NotEqualsCondition) cnd, ent),
    InCondition.class, (cnd, ent) -> handleIn((InCondition) cnd, ent),
    NotInCondition.class, (cnd, ent) -> handleNotIn((NotInCondition) cnd, ent),
    GreaterThanCondition.class, (cnd, ent) -> handleGreaterThan((GreaterThanCondition) cnd),
    LessThanCondition.class, (cnd, ent) -> handleLessThan((LessThanCondition) cnd),
    AndCondition.class, (cnd, ent) -> handleAnd((AndCondition) cnd, ent),
    RegexCondition.class, (cnd, ent) -> handleRegEx((RegexCondition) cnd)
  );

  public String getUserFriendlyQuery(FqlCondition<?> fqlCondition, EntityType entityType) {
    log.info("Computing user friendly query for fqlCondition: {}, entityType: {}", fqlCondition, entityType.getId());
    return userFriendlyQuery.get(fqlCondition.getClass()).apply(fqlCondition, entityType);
  }

  private String handleGreaterThan(GreaterThanCondition greaterThanCondition) {
    String operator = greaterThanCondition.orEqualTo() ? " >= " : " > ";
    return greaterThanCondition.fieldName() + operator + greaterThanCondition.value();
  }

  private String handleLessThan(LessThanCondition lessThanCondition) {
    String operator = lessThanCondition.orEqualTo() ? " <= " : " < ";
    return lessThanCondition.fieldName() + operator + lessThanCondition.value();
  }

  private String handleAnd(AndCondition andCondition, EntityType entityType) {
    return andCondition.value()
      .stream()
      .map(cnd -> this.getUserFriendlyQuery(cnd, entityType))
      .map(s -> '(' + s + ')')
      .collect(Collectors.joining(" AND "));
  }

  private String handleRegEx(RegexCondition regExCondition) {
    if (regExCondition.value().startsWith("^")) {
      return regExCondition.fieldName() + " starts with " + regExCondition.value().substring(1);
    }
    return regExCondition.fieldName() + " contains " + regExCondition.value();
  }

  private String handleEquals(EqualsCondition equalsCondition, EntityType entityType) {
    BiFunction<EntityTypeColumn, Object, String> labelFn = (col, val) -> this.getLabel(UUID.fromString(val.toString()), col);
    return handleConditionWithPossibleIdValue(equalsCondition, entityType, "==", labelFn);
  }

  private String handleNotEquals(NotEqualsCondition notEqualsCondition, EntityType entityType) {
    BiFunction<EntityTypeColumn, Object, String> labelFn = (col, val) -> this.getLabel(UUID.fromString(val.toString()), col);
    return handleConditionWithPossibleIdValue(notEqualsCondition, entityType, "!=", labelFn);
  }

  private String handleIn(InCondition inCondition, EntityType entityType) {
    BiFunction<EntityTypeColumn, List<Object>, String> labelFn = (col, val) -> {
      List<UUID> ids = val.stream().map(uuidStr -> UUID.fromString(uuidStr.toString())).toList();
      return getLabel(ids, col, true);
    };
    return handleConditionWithPossibleIdValue(inCondition, entityType, "in", labelFn);
  }

  private String handleNotIn(NotInCondition notInCondition, EntityType entityType) {
    BiFunction<EntityTypeColumn, List<Object>, String> labelFn = (col, val) -> {
      List<UUID> ids = val.stream().map(uuidStr -> UUID.fromString(uuidStr.toString())).toList();
      return getLabel(ids, col, true);
    };
    return handleConditionWithPossibleIdValue(notInCondition, entityType, "not in", labelFn);
  }

  private <T> String handleConditionWithPossibleIdValue(FieldCondition<T> condition,
                                                        EntityType entityType,
                                                        String userFriendlyOperator,
                                                        BiFunction<EntityTypeColumn, T, String> labelFn) {
    String operatorWithPadding = " " + userFriendlyOperator + " ";
    try {
      return entityType.getColumns()
        .stream()
        .filter(column -> column.getSource() != null && condition.fieldName().equals(column.getIdColumnName()))
        .findFirst()
        .map(column -> column.getName() + operatorWithPadding + labelFn.apply(column, condition.value()))
        .orElse(condition.fieldName() + operatorWithPadding + condition.value());
    } catch (Exception e) {
      log.error("Unexpected error when creating user friendly query for condition " + condition);
      return condition.fieldName() + operatorWithPadding + condition.value();
    }
  }

  private String getLabel(UUID id, EntityTypeColumn column) {
    return getLabel(List.of(id), column, false);
  }

  private String getLabel(List<UUID> ids, EntityTypeColumn column, Boolean addBrackets) {
    UUID sourceEntityTypeId = UUID.fromString(column.getSource().getEntityTypeId());
    var collector = Boolean.TRUE.equals(addBrackets) ? Collectors.joining(", ", "[", "]") :
      Collectors.joining(",");
    return resultSetService.getResultSet(executionContext.getTenantId(),
        sourceEntityTypeId,
        List.of("id", column.getSource().getColumnName()),
        ids)
      .stream()
      .map(map -> map.get(column.getSource().getColumnName()).toString())
      .collect(collector);
  }
}
