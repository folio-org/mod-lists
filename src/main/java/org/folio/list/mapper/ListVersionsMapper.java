package org.folio.list.mapper;

import org.folio.list.domain.ListVersions;
import org.folio.list.domain.dto.ListDTO;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR,
  uses = {MappingMethods.class, ListRefreshMapper.class}, builder = @Builder(disableBuilder = true))
public interface ListVersionsMapper {
  @Mapping(target = "id", source = "listVersions.id")
  @Mapping(target = "listId", source = "listVersions.list.id")
  @Mapping(target = "name", source = "listVersions.name")
  @Mapping(target = "description", source = "listVersions.description")
  @Mapping(target = "userFriendlyQuery", source = "listVersions.userFriendlyQuery")
  @Mapping(target = "fqlQuery", source = "listVersions.fqlQuery")
  @Mapping(target = "fields", source = "listVersions.fields")
  @Mapping(target = "isActive", source = "listVersions.isActive")
  @Mapping(target = "isPrivate", source = "listVersions.isPrivate")
  @Mapping(target = "updatedBy", source = "listVersions.updatedBy")
  @Mapping(target = "updatedByUsername", source = "listVersions.updatedByUsername")
  @Mapping(target = "updatedDate", source = "listVersions.updatedDate")
  @Mapping(target = "version", source = "listVersions.version")
  org.folio.list.domain.dto.ListVersionsDTO toListVersionsDTO(ListVersions listVersions);
}
