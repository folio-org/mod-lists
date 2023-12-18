package org.folio.list.mapper;

import org.folio.list.domain.ListVersion;
import org.folio.list.domain.dto.ListVersionDTO;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR,
  uses = {MappingMethods.class, ListRefreshMapper.class}, builder = @Builder(disableBuilder = true))
public interface ListVersionMapper {
  @Mapping(target = "id", source = "listVersion.id")
  @Mapping(target = "listId", source = "listVersion.listId")
  @Mapping(target = "name", source = "listVersion.name")
  @Mapping(target = "description", source = "listVersion.description")
  @Mapping(target = "userFriendlyQuery", source = "listVersion.userFriendlyQuery")
  @Mapping(target = "fqlQuery", source = "listVersion.fqlQuery")
  @Mapping(target = "fields", source = "listVersion.fields")
  @Mapping(target = "isActive", source = "listVersion.isActive")
  @Mapping(target = "isPrivate", source = "listVersion.isPrivate")
  @Mapping(target = "updatedBy", source = "listVersion.updatedBy")
  @Mapping(target = "updatedByUsername", source = "listVersion.updatedByUsername")
  @Mapping(target = "updatedDate", source = "listVersion.updatedDate")
  @Mapping(target = "version", source = "listVersion.version")
  ListVersionDTO toListVersionDTO(ListVersion listVersion);
}
