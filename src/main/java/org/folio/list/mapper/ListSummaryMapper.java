package org.folio.list.mapper;

import org.folio.list.domain.dto.ListSummaryDTO;
import org.folio.list.domain.ListEntity;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR,
  uses = MappingMethods.class, builder = @Builder(disableBuilder = true))
public interface ListSummaryMapper {
  @Mapping(target = "name", source = "list.name")
  @Mapping(target = "entityTypeId", source = "list.entityTypeId")
  @Mapping(target = "createdByUsername", source = "list.createdByUsername")
  @Mapping(target = "createdDate", source = "list.createdDate")
  @Mapping(target = "isActive", source = "list.isActive")
  @Mapping(target = "isPrivate", source = "list.isPrivate")
  @Mapping(target = "isCanned", source = "list.isCanned")
  @Mapping(target = "updatedBy", source = "list.updatedBy")
  @Mapping(target = "updatedByUsername", source = "list.updatedByUsername")
  @Mapping(target = "updatedDate", source = "list.updatedDate")
  @Mapping(target = "recordsCount", source = "list.successRefresh.recordsCount")
  @Mapping(target = "refreshedDate", source = "list.successRefresh.refreshEndDate")
  @Mapping(target = "refreshedByUsername", source = "list.successRefresh.refreshedByUsername")
  @Mapping(target = "isRefreshing", expression = "java(list.isRefreshing())")
  @Mapping(target = "latestRefreshFailed", expression = "java(list.refreshFailed())")
  ListSummaryDTO toListSummaryDTO(ListEntity list);
}
