package org.folio.list.mapper;

import org.folio.list.domain.dto.ListDTO;
import org.folio.list.domain.ListEntity;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR,
  uses = {MappingMethods.class, ListRefreshMapper.class}, builder = @Builder(disableBuilder = true))
public interface ListMapper {
  @Mapping(target = "name", source = "entity.name")
  @Mapping(target = "entityTypeId", source = "entity.entityTypeId")
  @Mapping(target = "fqlQuery", source = "entity.fqlQuery")
  @Mapping(target = "fields", source = "entity.fields")
  @Mapping(target = "createdBy", source = "entity.createdBy")
  @Mapping(target = "createdByUsername", source = "entity.createdByUsername")
  @Mapping(target = "createdDate", source = "entity.createdDate")
  @Mapping(target = "isActive", source = "entity.isActive")
  @Mapping(target = "isPrivate", source = "entity.isPrivate")
  @Mapping(target = "isCanned", source = "entity.isCanned")
  @Mapping(target = "updatedBy", source = "entity.updatedBy")
  @Mapping(target = "updatedByUsername", source = "entity.updatedByUsername")
  @Mapping(target = "updatedDate", source = "entity.updatedDate")
  @Mapping(target = "successRefresh", source = "entity.successRefresh")
  @Mapping(target = "inProgressRefresh", source = "entity.inProgressRefresh")
  @Mapping(target = "failedRefresh", source = "entity.failedRefresh")
  @Mapping(target = "version", source = "entity.version")
  ListDTO toListDTO(ListEntity entity);
}
