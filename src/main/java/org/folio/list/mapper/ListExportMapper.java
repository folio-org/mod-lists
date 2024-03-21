package org.folio.list.mapper;

import org.folio.list.domain.ExportDetails;
import org.mapstruct.*;
import org.folio.list.domain.dto.ListExportDTO;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR,
  uses = MappingMethods.class, builder = @Builder(disableBuilder = true))
public interface ListExportMapper {

  @Mapping(target = "exportId", source = "exportDetails.exportId")
  @Mapping(target = "listId", source = "exportDetails.list.id")
  @Mapping(target = "status", source = "exportDetails.status")
  @Mapping(target = "createdBy", source = "exportDetails.createdBy")
  @Mapping(target = "startDate", source = "exportDetails.startDate")
  @Mapping(target = "endDate", source = "exportDetails.endDate")
  @Mapping(target = "fields", source = "exportDetails.fields")
  ListExportDTO toListExportDTO(ExportDetails exportDetails);
}
