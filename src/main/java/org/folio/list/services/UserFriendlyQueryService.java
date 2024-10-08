package org.folio.list.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.fql.model.*;
import org.folio.fql.service.FqlService;
import org.folio.fql.service.FqlValidationService;
import org.folio.list.domain.ListEntity;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.rest.QueryClient;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.Field;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


@Log4j2
@Service
@RequiredArgsConstructor
public class UserFriendlyQueryService {

  private final EntityTypeClient entityTypeClient;
  private final FqlService fqlService;
  private final QueryClient queryClient;

  private final Map<Class<? extends FqlCondition<?>>, BiFunction<FqlCondition<?>, EntityType, String>> userFriendlyQuery = Map.ofEntries(
    Map.entry(EqualsCondition.class, (cnd, ent) -> handleEquals((EqualsCondition) cnd, ent)),
    Map.entry(NotEqualsCondition.class, (cnd, ent) -> handleNotEquals((NotEqualsCondition) cnd, ent)),
    Map.entry(InCondition.class, (cnd, ent) -> handleIn((InCondition) cnd, ent)),
    Map.entry(NotInCondition.class, (cnd, ent) -> handleNotIn((NotInCondition) cnd, ent)),
    Map.entry(GreaterThanCondition.class, (cnd, ent) -> handleGreaterThan((GreaterThanCondition) cnd)),
    Map.entry(LessThanCondition.class, (cnd, ent) -> handleLessThan((LessThanCondition) cnd)),
    Map.entry(AndCondition.class, (cnd, ent) -> handleAnd((AndCondition) cnd, ent)),
    Map.entry(RegexCondition.class, (cnd, ent) -> handleRegEx((RegexCondition) cnd)),
    Map.entry(ContainsAllCondition.class, (cnd, ent) -> handleContainsAll((ContainsAllCondition) cnd, ent)),
    Map.entry(NotContainsAllCondition.class, (cnd, ent) -> handleNotContainsAll((NotContainsAllCondition) cnd, ent)),
    Map.entry(ContainsAnyCondition.class, (cnd, ent) -> handleContainsAny((ContainsAnyCondition) cnd, ent)),
    Map.entry(NotContainsAnyCondition.class, (cnd, ent) -> handleNotContainsAny((NotContainsAnyCondition) cnd, ent)),
    Map.entry(EmptyCondition.class, (cnd, ent) -> handleEmpty((EmptyCondition) cnd))
  );

  public String getUserFriendlyQuery(FqlCondition<?> fqlCondition, EntityType entityType) {
    log.info("Computing user friendly query for fqlCondition: {}, entityType: {}", fqlCondition, entityType.getId());
    return userFriendlyQuery.get(fqlCondition.getClass()).apply(fqlCondition, entityType);
  }

  public String getUserFriendlyQuery(String fqlCriteria, EntityType entityType) {
    Fql fql = fqlService.getFql(fqlCriteria);
    return getUserFriendlyQuery(fql.fqlCondition(), entityType);
  }

  public void updateListUserFriendlyQuery(ListEntity listEntity) {
    listEntity.setUserFriendlyQuery(
      getUserFriendlyQuery(listEntity.getFqlQuery(), entityTypeClient.getEntityType(listEntity.getEntityTypeId(), ListActions.UPDATE))
    );
  }

  public void updateListUserFriendlyQuery(ListEntity listEntity, EntityType entityType) {
    listEntity.setUserFriendlyQuery(
      getUserFriendlyQuery(listEntity.getFqlQuery(), entityType)
    );
  }

  private String handleGreaterThan(GreaterThanCondition greaterThanCondition) {
    String operator = greaterThanCondition.orEqualTo() ? " >= " : " > ";
    return greaterThanCondition.field().serialize() + operator + greaterThanCondition.value();
  }

  private String handleLessThan(LessThanCondition lessThanCondition) {
    String operator = lessThanCondition.orEqualTo() ? " <= " : " < ";
    return lessThanCondition.field().serialize()  + operator + lessThanCondition.value();
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
      return regExCondition.field().serialize()  + " starts with " + regExCondition.value().substring(1);
    }
    return regExCondition.field().serialize()  + " contains " + regExCondition.value();
  }

  private String handleEquals(EqualsCondition equalsCondition, EntityType entityType) {
    BiFunction<Field, Object, String> labelFn = (col, val) -> this.getLabel(UUID.fromString(val.toString()), col);
    return handleConditionWithPossibleIdValue(equalsCondition, entityType, "==", labelFn);
  }

  private String handleNotEquals(NotEqualsCondition notEqualsCondition, EntityType entityType) {
    BiFunction<Field, Object, String> labelFn = (col, val) -> this.getLabel(UUID.fromString(val.toString()), col);
    return handleConditionWithPossibleIdValue(notEqualsCondition, entityType, "!=", labelFn);
  }

  private String handleIn(InCondition inCondition, EntityType entityType) {
    BiFunction<Field, List<Object>, String> labelFn = (col, val) -> {
      List<List<String>> ids = val.stream().map(uuidStr -> List.of(uuidStr.toString())).toList();
      return getLabel(ids, col, true);
    };
    return handleConditionWithPossibleIdValue(inCondition, entityType, "in", labelFn);
  }

  private String handleNotIn(NotInCondition notInCondition, EntityType entityType) {
    BiFunction<Field, List<Object>, String> labelFn = (col, val) -> {
      List<List<String>> ids = val.stream().map(uuidStr -> List.of(uuidStr.toString())).toList();
      return getLabel(ids, col, true);
    };
    return handleConditionWithPossibleIdValue(notInCondition, entityType, "not in", labelFn);
  }

  private String handleContainsAll(ContainsAllCondition containsAllCondition, EntityType entityType) {
    BiFunction<Field, List<Object>, String> labelFn = (col, val) -> {
      List<List<String>> ids = val.stream().map(uuidStr -> List.of(uuidStr.toString())).toList();
      return getLabel(ids, col, true);
    };
    return handleConditionWithPossibleIdValue(containsAllCondition, entityType, "contains all", labelFn);
  }

  private String handleNotContainsAll(NotContainsAllCondition notContainsAllCondition, EntityType entityType) {
    BiFunction<Field, List<Object>, String> labelFn = (col, val) -> {
      List<List<String>> ids = val.stream().map(uuidStr -> List.of(uuidStr.toString())).toList();
      return getLabel(ids, col, true);
    };
    return handleConditionWithPossibleIdValue(notContainsAllCondition, entityType, "does not contain all", labelFn);
  }

  private String handleContainsAny(ContainsAnyCondition containsAnyCondition, EntityType entityType) {
    BiFunction<Field, List<Object>, String> labelFn = (col, val) -> {
      List<List<String>> ids = val.stream().map(uuidStr -> List.of(uuidStr.toString())).toList();
      return getLabel(ids, col, true);
    };
    return handleConditionWithPossibleIdValue(containsAnyCondition, entityType, "contains any", labelFn);
  }

  private String handleNotContainsAny(NotContainsAnyCondition notContainsAnyCondition, EntityType entityType) {
    BiFunction<Field, List<Object>, String> labelFn = (col, val) -> {
      List<List<String>> ids = val.stream().map(uuidStr -> List.of(uuidStr.toString())).toList();
      return getLabel(ids, col, true);
    };
    return handleConditionWithPossibleIdValue(notContainsAnyCondition, entityType, "does not contain any", labelFn);
  }

  private String handleEmpty(EmptyCondition emptyCondition) {
    if (Boolean.TRUE.equals(emptyCondition.value())) {
      return emptyCondition.field().serialize()  + " is empty";
    } else {
      return emptyCondition.field().serialize()  + " is not empty";
    }
  }

  private <T> String handleConditionWithPossibleIdValue(FieldCondition<T> condition,
                                                        EntityType entityType,
                                                        String userFriendlyOperator,
                                                        BiFunction<Field, T, String> labelFn) {
    String operatorWithPadding = " " + userFriendlyOperator + " ";
    try {
      // the field referenced directly in the query. May or may not have a source or ID column
      // e.g. user patron group, vendor code, vendor ID, etc
      Field querySubjectField = FqlValidationService.findFieldDefinition(condition.field(), entityType).orElseThrow();
      if (querySubjectField.getIdColumnName() != null) {
        return condition.field().serialize() + operatorWithPadding + labelFn.apply(querySubjectField, condition.value());
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
          .orElse(condition.field().serialize() + operatorWithPadding + condition.value());
      }
    } catch (Exception e) {
      log.error("Unexpected error when creating user friendly query for condition " + condition + ". Exception: " + e);
      return condition.field().serialize() + operatorWithPadding + condition.value();
    }
  }

  private String getLabel(UUID id, Field column) {
    return getLabel(List.of(List.of(id.toString())), column, false);
  }

  private String getLabel(List<List<String>> ids, Field field, Boolean addBrackets) {
    log.info("Getting label for ids: {} on field {}", ids, field.getName());
    UUID sourceEntityTypeId = field.getSource().getEntityTypeId();
    var collector = Boolean.TRUE.equals(addBrackets) ? Collectors.joining(", ", "[", "]") :
      Collectors.joining(",");
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(sourceEntityTypeId)
      .fields(List.of("id", field.getSource().getColumnName()))
      .ids(ids);
    return queryClient.getContents(contentsRequest)
      .stream()
      .map(map -> map.get(field.getSource().getColumnName()).toString())
      .collect(collector);
  }
}
