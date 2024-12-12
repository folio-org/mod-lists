package org.folio.list.mapper;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;
import org.folio.list.domain.ListEntity;
import org.folio.list.services.UserFriendlyQueryService;
import org.folio.querytool.domain.dto.FqmMigrateRequest;
import org.folio.querytool.domain.dto.FqmMigrateResponse;
import org.folio.querytool.domain.dto.FqmMigrateWarning;
import org.folio.spring.i18n.service.TranslationService;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
  componentModel = MappingConstants.ComponentModel.SPRING,
  injectionStrategy = InjectionStrategy.CONSTRUCTOR,
  uses = { TranslationService.class, UserFriendlyQueryService.class }
)
// we cannot use constructor injection in the subclass due to https://github.com/mapstruct/mapstruct/issues/2257
// and we cannot use an interface here, due to the @AfterMapping.
// so, we use field injection here.
@SuppressWarnings("java:S6813")
public abstract class ListMigrationMapper {

  @Autowired
  private TranslationService translationService;

  @Autowired
  private UserFriendlyQueryService userFriendlyQueryService;

  public abstract FqmMigrateRequest toMigrationRequest(ListEntity list);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "entityTypeId", source = "response.entityTypeId")
  @Mapping(target = "fqlQuery", source = "response.fqlQuery")
  @Mapping(target = "fields", source = "response.fields")
  // description is mapped below
  public abstract ListEntity updateListWithMigration(@MappingTarget ListEntity source, FqmMigrateResponse response);

  // there's no way to do a @Mapping with two sources, unfortunately
  // https://github.com/mapstruct/mapstruct/issues/621
  // instead, we have to do this at the end and manually implement this mapping :/
  @AfterMapping
  protected void updateListDescriptionAfterMigration(@MappingTarget ListEntity list, FqmMigrateResponse response) {
    userFriendlyQueryService.updateListUserFriendlyQuery(list);

    if (response.getWarnings().isEmpty()) {
      return;
    }

    list.setDescription(
      (
        Optional.ofNullable(list.getDescription()).orElse("") +
        "\n\n" +
        translationService.format(
          "mod-lists.migration.warning-header",
          "date",
          Instant.now(),
          "count",
          response.getWarnings().size()
        ) +
        "\n" +
        response.getWarnings().stream().map(FqmMigrateWarning::getDescription).collect(Collectors.joining("\n"))
      ).trim()
    );
  }
}
