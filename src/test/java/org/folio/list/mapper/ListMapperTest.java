package org.folio.list.mapper;

import lombok.SneakyThrows;
import org.folio.list.domain.dto.ListDTO;
import org.folio.list.domain.dto.ListRefreshDTO;
import org.folio.list.mapper.ListRefreshMapperImpl;
import org.folio.list.utils.TestDataFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest(classes = {ListMapperImpl.class, MappingMethods.class, ListRefreshMapperImpl.class})
@Transactional
class ListMapperTest {
  @Autowired
  private MappingMethods mappingMethods;

  @Autowired
  private ListRefreshMapper listRefreshMapper;

  @Autowired
  private ListMapper mapper;

  @Test
  @SneakyThrows
  void shouldMapEntityToDtoWithSuccessRefresh() {
    UUID listId = UUID.randomUUID();
    var listEntity = TestDataFixture.getListEntityWithSuccessRefresh(listId);

    ListDTO dto = mapper.toListDTO(listEntity);
    assertEquals(dto.getId(), listEntity.getId());
    assertEquals(dto.getName(), listEntity.getName());
    assertEquals(dto.getEntityTypeId(), listEntity.getEntityTypeId());
    assertEquals(dto.getDescription(), listEntity.getDescription());
    assertEquals(dto.getFqlQuery(), listEntity.getFqlQuery());
    assertEquals(dto.getCreatedByUsername(), listEntity.getCreatedByUsername());
    assertEquals(dto.getIsActive(), listEntity.getIsActive());
    assertEquals(dto.getIsPrivate(), listEntity.getIsPrivate());
    assertEquals(dto.getCreatedBy(), listEntity.getCreatedBy());
    assertEquals(dto.getUpdatedBy(), listEntity.getUpdatedBy());
    assertEquals(dto.getUpdatedByUsername(), listEntity.getUpdatedByUsername());
    assertEquals(dto.getVersion(), listEntity.getVersion());
    assertEquals(dto.getUpdatedDate(), mappingMethods.offsetDateTimeAsDate(listEntity.getUpdatedDate()));
    assertEquals(dto.getSuccessRefresh().getId(), listEntity.getSuccessRefresh().getId());
    assertEquals(dto.getSuccessRefresh().getListId(), listEntity.getSuccessRefresh().getListId());
    assertEquals(ListRefreshDTO.StatusEnum.SUCCESS, dto.getSuccessRefresh().getStatus());
    assertEquals(dto.getSuccessRefresh().getRefreshStartDate(), mappingMethods.offsetDateTimeAsDate(
      listEntity.getSuccessRefresh().getRefreshStartDate()));
    assertEquals(dto.getSuccessRefresh().getRefreshEndDate(), mappingMethods.offsetDateTimeAsDate(
      listEntity.getSuccessRefresh().getRefreshEndDate()));
    assertEquals(dto.getSuccessRefresh().getRefreshedBy(), listEntity.getSuccessRefresh().getRefreshedBy());
    assertEquals(dto.getSuccessRefresh().getRefreshedByUsername(), listEntity.getSuccessRefresh().getRefreshedByUsername());
    assertEquals(dto.getSuccessRefresh().getRecordsCount(), listEntity.getSuccessRefresh().getRecordsCount());
  }

  @Test
  @SneakyThrows
  void shouldMapEntityToDtoWithInProgressRefresh() {
    UUID listId = UUID.randomUUID();
    var listEntity = TestDataFixture.getListEntityWithInProgressRefresh(listId);

    ListDTO dto = mapper.toListDTO(listEntity);
    assertEquals(dto.getId(), listEntity.getId());
    assertEquals(dto.getName(), listEntity.getName());
    assertEquals(dto.getEntityTypeId(), listEntity.getEntityTypeId());
    assertEquals(dto.getDescription(), listEntity.getDescription());
    assertEquals(dto.getFqlQuery(), listEntity.getFqlQuery());
    assertEquals(dto.getCreatedByUsername(), listEntity.getCreatedByUsername());
    assertEquals(dto.getIsActive(), listEntity.getIsActive());
    assertEquals(dto.getIsPrivate(), listEntity.getIsPrivate());
    assertEquals(dto.getCreatedBy(), listEntity.getCreatedBy());
    assertEquals(dto.getUpdatedBy(), listEntity.getUpdatedBy());
    assertEquals(dto.getUpdatedByUsername(), listEntity.getUpdatedByUsername());
    assertEquals(dto.getVersion(), listEntity.getVersion());
    assertEquals(dto.getUpdatedDate(), mappingMethods.offsetDateTimeAsDate(listEntity.getUpdatedDate()));
    assertEquals(dto.getInProgressRefresh().getId(), listEntity.getInProgressRefresh().getId());
    assertEquals(dto.getInProgressRefresh().getListId(), listEntity.getInProgressRefresh().getListId());
    assertEquals(ListRefreshDTO.StatusEnum.IN_PROGRESS, dto.getInProgressRefresh().getStatus());
    assertEquals(dto.getInProgressRefresh().getRefreshStartDate(), mappingMethods.offsetDateTimeAsDate(
      listEntity.getInProgressRefresh().getRefreshStartDate()));
    assertEquals(dto.getInProgressRefresh().getRefreshEndDate(), mappingMethods.offsetDateTimeAsDate(
      listEntity.getInProgressRefresh().getRefreshEndDate()));
    assertEquals(dto.getInProgressRefresh().getRefreshedBy(), listEntity.getInProgressRefresh().getRefreshedBy());
    assertEquals(dto.getInProgressRefresh().getRefreshedByUsername(), listEntity.getInProgressRefresh().getRefreshedByUsername());
    assertEquals(dto.getInProgressRefresh().getRecordsCount(), listEntity.getInProgressRefresh().getRecordsCount());
  }

  @Test
  @SneakyThrows
  void shouldMapEntityToDtoWithFailedRefresh() {
    var listEntity = TestDataFixture.getListEntityFailedRefresh();

    ListDTO dto = mapper.toListDTO(listEntity);
    assertEquals(dto.getId(), listEntity.getId());
    assertEquals(dto.getName(), listEntity.getName());
    assertEquals(dto.getEntityTypeId(), listEntity.getEntityTypeId());
    assertEquals(dto.getDescription(), listEntity.getDescription());
    assertEquals(dto.getFqlQuery(), listEntity.getFqlQuery());
    assertEquals(dto.getCreatedByUsername(), listEntity.getCreatedByUsername());
    assertEquals(dto.getIsActive(), listEntity.getIsActive());
    assertEquals(dto.getIsPrivate(), listEntity.getIsPrivate());
    assertEquals(dto.getCreatedBy(), listEntity.getCreatedBy());
    assertEquals(dto.getUpdatedBy(), listEntity.getUpdatedBy());
    assertEquals(dto.getUpdatedByUsername(), listEntity.getUpdatedByUsername());
    assertEquals(dto.getVersion(), listEntity.getVersion());
    assertEquals(dto.getUpdatedDate(), mappingMethods.offsetDateTimeAsDate(listEntity.getUpdatedDate()));
    assertEquals(dto.getFailedRefresh().getId(), listEntity.getFailedRefresh().getId());
    assertEquals(dto.getFailedRefresh().getListId(), listEntity.getFailedRefresh().getListId());
    assertEquals(ListRefreshDTO.StatusEnum.FAILED, dto.getFailedRefresh().getStatus());
    assertEquals(dto.getFailedRefresh().getRefreshStartDate(), mappingMethods.offsetDateTimeAsDate(
      listEntity.getFailedRefresh().getRefreshStartDate()));
    assertEquals(dto.getFailedRefresh().getRefreshEndDate(), mappingMethods.offsetDateTimeAsDate(
      listEntity.getFailedRefresh().getRefreshEndDate()));
    assertEquals(dto.getFailedRefresh().getRefreshedBy(), listEntity.getFailedRefresh().getRefreshedBy());
    assertEquals(dto.getFailedRefresh().getRefreshedByUsername(), listEntity.getFailedRefresh().getRefreshedByUsername());
    assertEquals(dto.getFailedRefresh().getRecordsCount(), listEntity.getFailedRefresh().getRecordsCount());
  }
}
