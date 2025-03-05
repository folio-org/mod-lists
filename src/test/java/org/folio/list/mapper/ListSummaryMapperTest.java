package org.folio.list.mapper;

import org.folio.list.domain.dto.ListSummaryDTO;
import org.folio.list.domain.ListEntity;
import org.folio.list.utils.TestDataFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest(classes = {ListSummaryMapperImpl.class, MappingMethods.class})
@Transactional
class ListSummaryMapperTest {

  @Autowired
  private ListSummaryMapper listSummaryMapper;

  @Test
  void shouldMapEntityToDto() {
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh(UUID.randomUUID());
    ListSummaryDTO dto = listSummaryMapper.toListSummaryDTO(entity, "Item");
    assertEquals(dto.getName(), entity.getName());
    assertEquals(dto.getEntityTypeId(), entity.getEntityTypeId());
    assertEquals("Item", dto.getEntityTypeName());
    assertEquals(dto.getCreatedByUsername(), entity.getCreatedByUsername());
    assertEquals(dto.getIsPrivate(), entity.getIsPrivate());
    assertEquals(dto.getLatestRefreshFailed(), entity.refreshFailed());
  }
}
