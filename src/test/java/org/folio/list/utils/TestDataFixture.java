package org.folio.list.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.folio.list.domain.ExportDetails;
import org.folio.list.domain.dto.ListDTO;
import org.folio.list.domain.dto.ListSummaryDTO;
import org.folio.list.domain.dto.ListRefreshDTO;
import org.folio.list.domain.dto.ListRequestDTO;
import org.folio.list.domain.dto.ListUpdateRequestDTO;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.ListRefreshDetails;
import org.folio.list.domain.dto.ListExportDTO;

import java.util.UUID;

public class TestDataFixture {

  private static final ObjectMapper objectMapper;

  static {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @SneakyThrows
  public static ListEntity getListEntityWithSuccessRefresh(UUID listId) {
    var listEntity = getListEntityWithSuccessRefresh();
    listEntity.setId(listId);
    return listEntity;
  }

  @SneakyThrows
  public static ListEntity getListEntityWithSuccessRefresh() {
    var resource = TestDataFixture.class.getResource("/json/list/list-success-refresh.json");
    return objectMapper.readValue(resource, ListEntity.class);
  }

  public static ListEntity getListEntityWithInProgressRefresh(UUID listId) {
    return getListEntityWithInProgressRefresh();
  }

  @SneakyThrows
  public static ListEntity getListEntityWithInProgressRefresh() {
    var resource = TestDataFixture.class.getResource("/json/list/list-inprogress.json");
    return objectMapper.readValue(resource, ListEntity.class);
  }

  public static ListEntity getNeverRefreshedListEntity() {
    return getListEntityWithInProgressRefresh();
  }

  @SneakyThrows
  public static ListEntity getListEntityWithInProgressAndSuccessRefresh() {
    var resource = TestDataFixture.class.getResource("/json/list/list-in-progress-with-success-refresh.json");
    return objectMapper.readValue(resource, ListEntity.class);
  }

  @SneakyThrows
  public static ListDTO getListDTOSuccessRefresh(UUID listId) {
    var resource = TestDataFixture.class.getResource("/json/list/list-success-refresh.json");
    var listDto = objectMapper.readValue(resource, ListDTO.class);
    listDto.setId(listId);
    return listDto;
  }

  @SneakyThrows
  public static ListDTO getListDTOInProgressRefresh(UUID listId) {
    var resource = TestDataFixture.class.getResource("/json/list/list-inprogress.json");
    var listDto = objectMapper.readValue(resource, ListDTO.class);
    listDto.setId(listId);
    return listDto;
  }

  @SneakyThrows
  public static ListSummaryDTO getListSummaryDTO(UUID listId) {
    var resource = TestDataFixture.class.getResource("/json/list/list-summary.json");
    var listSummaryDto = objectMapper.readValue(resource, ListSummaryDTO.class);
    listSummaryDto.setId(listId);
    return listSummaryDto;
  }

  @SneakyThrows
  public static ListRefreshDTO getListRefreshDTO() {
    var resource = TestDataFixture.class.getResource("/json/list/list-refresh.json");
    return objectMapper.readValue(resource, ListRefreshDTO.class);
  }

  @SneakyThrows
  public static ListRefreshDetails getListRefreshDetails() {
    var resource = TestDataFixture.class.getResource("/json/list/list-refresh.json");
    return objectMapper.readValue(resource, ListRefreshDetails.class);
  }

  @SneakyThrows
  public static ListDTO getListDTOFailedRefresh(UUID listId) {
    var resource = TestDataFixture.class.getResource("/json/list/list-failed-refresh.json");
    var listDTO = objectMapper.readValue(resource, ListDTO.class);
    listDTO.setId(listId);
    return listDTO;
  }

  @SneakyThrows
  public static ListEntity getListEntityFailedRefresh() {
    var resource = TestDataFixture.class.getResource("/json/list/list-failed-refresh.json");
    return objectMapper.readValue(resource, ListEntity.class);
  }

  @SneakyThrows
  public static ListEntity getInactiveListEntity() {
    var resource = TestDataFixture.class.getResource("/json/list/list-inactive.json");
    return objectMapper.readValue(resource, ListEntity.class);
  }

  @SneakyThrows
  public static ListEntity getListEntityWithoutQuery() {
    var resource = TestDataFixture.class.getResource("/json/list/list-without-query.json");
    return objectMapper.readValue(resource, ListEntity.class);
  }

  @SneakyThrows
  public static ListRequestDTO getListRequestDTO() {
    var resource = TestDataFixture.class.getResource("/json/list/create-list.json");
    return objectMapper.readValue(resource, ListRequestDTO.class);
  }

  @SneakyThrows
  public static ListUpdateRequestDTO getListUpdateRequestDTO() {
    var resource = TestDataFixture.class.getResource("/json/list/update-list.json");
    return objectMapper.readValue(resource, ListUpdateRequestDTO.class);
  }

  @SneakyThrows
  public static ListEntity getPrivateListEntity() {
    var resource = TestDataFixture.class.getResource("/json/list/list-success-refresh.json");
    var listEntity = objectMapper.readValue(resource, ListEntity.class);
    listEntity.setIsPrivate(true);
    listEntity.setIsCanned(false);
    return listEntity;
  }

  @SneakyThrows
  public static ListEntity getSharedNonCannedListEntity() {
    var resource = TestDataFixture.class.getResource("/json/list/list-success-refresh.json");
    var listEntity = objectMapper.readValue(resource, ListEntity.class);
    listEntity.setIsPrivate(false);
    listEntity.setIsCanned(false);
    return listEntity;
  }

  @SneakyThrows
  public static ListDTO getPrivateListDTO() {
    var resource = TestDataFixture.class.getResource("/json/list/list-success-refresh.json");
    var listDto = objectMapper.readValue(resource, ListDTO.class);
    listDto.setIsPrivate(true);
    return listDto;
  }

  @SneakyThrows
  public static ExportDetails getListExportDetails() {
    var resource = TestDataFixture.class.getResource("/json/list/list-export-details.json");
    return objectMapper.readValue(resource, ExportDetails.class);
  }

  @SneakyThrows
  public static ListExportDTO getListExportDTO() {
    var resource = TestDataFixture.class.getResource("/json/list/list-export-details.json");
    return objectMapper.readValue(resource, ListExportDTO.class);
  }
}
