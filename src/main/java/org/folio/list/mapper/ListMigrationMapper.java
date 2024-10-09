package org.folio.list.mapper;

import java.time.Instant;
import java.util.stream.Collectors;
import org.folio.list.domain.ListEntity;
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
  uses = { MappingMethods.class, ListRefreshMapper.class }
)
public abstract class ListMigrationMapper {

  @Autowired
  TranslationService translationService;

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
    if (response.getWarnings().isEmpty()) {
      return;
    }

    list.setDescription(
      (
        list.getDescription() +
        "\n\n" +
        translationService.format("mod-lists.migration.warning-header", "date", Instant.now()) +
        "\n" +
        response.getWarnings().stream().map(FqmMigrateWarning::getDescription).collect(Collectors.joining("\n"))
      ).trim()
    );
  }
}
