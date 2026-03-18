package org.folio.list.mapper;

import org.folio.list.domain.dto.ListAppError;
import org.folio.list.domain.dto.ListRefreshDTO;
import org.folio.list.domain.ListRefreshDetails;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR,
  uses = MappingMethods.class, builder = @Builder(disableBuilder = true))
public interface ListRefreshMapper {
  @Mapping(target = "id", source = "listRefreshDetails.id")
  @Mapping(target = "listId", source = "listRefreshDetails.listId")
  @Mapping(target = "status", source = "listRefreshDetails.status")
  @Mapping(target = "refreshStartDate", source = "listRefreshDetails.refreshStartDate")
  @Mapping(target = "refreshEndDate", source = "listRefreshDetails.refreshEndDate")
  @Mapping(target = "refreshedBy", source = "listRefreshDetails.refreshedBy")
  @Mapping(target = "refreshedByUsername", source = "listRefreshDetails.refreshedByUsername")
  @Mapping(target = "recordsCount", source = "listRefreshDetails.recordsCount")
  @Mapping(target = "contentVersion", source = "listRefreshDetails.contentVersion")
  @Mapping(target = "error", expression = "java(convertToListAppError(listRefreshDetails))")
  @Mapping(target = "listVersion", source = "listRefreshDetails.listVersion")
  ListRefreshDTO toListRefreshDTO(ListRefreshDetails listRefreshDetails);

  default ListAppError convertToListAppError(ListRefreshDetails listRefreshDetails) {
    if (listRefreshDetails.getErrorCode() != null && listRefreshDetails.getErrorMessage() != null) {
      return new ListAppError()
        .code(listRefreshDetails.getErrorCode())
        .message(listRefreshDetails.getErrorMessage());
    }
    return null;
  }
}
