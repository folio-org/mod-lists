package org.folio.list.mapper;

import org.folio.list.domain.ListEntity;
import org.folio.list.domain.dto.ListRequestDTO;
import org.folio.list.rest.UsersClient;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR,
  builder = @Builder(disableBuilder = true))
public interface ListEntityMapper {
  @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
  @Mapping(target = "name", source = "request.name")
  @Mapping(target = "entityTypeId", source = "request.entityTypeId")
  @Mapping(target = "description", source = "request.description")
  @Mapping(target = "fqlQuery", source = "request.fqlQuery")
  @Mapping(target = "isActive", source = "request.isActive")
  @Mapping(target = "isPrivate", source = "request.isPrivate")
  @Mapping(target = "createdDate", expression = "java(java.time.OffsetDateTime.now())")
  @Mapping(target = "createdBy", expression = "java(createdBy.id())")
  @Mapping(target = "createdByUsername", expression = "java(createdBy.getFullName().orElse(createdBy.id().toString()))")
  @Mapping(target = "updatedDate", expression = "java(java.time.OffsetDateTime.now())")
  @Mapping(target = "updatedBy", expression = "java(createdBy.id())")
  @Mapping(target = "updatedByUsername", expression = "java(createdBy.getFullName().orElse(createdBy.id().toString()))")
  @Mapping(target = "isCanned", constant = "false")
  @Mapping(target = "version", constant = "1")
  ListEntity toListEntity(ListRequestDTO request, UsersClient.User createdBy);
}
