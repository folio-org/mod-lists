package org.folio.list.mapper;

import org.folio.list.domain.ListVersion;
import org.folio.list.domain.dto.ListVersionDTO;
import org.folio.list.utils.TestDataFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {org.folio.list.mapper.ListVersionMapperImpl.class, MappingMethods.class})
@Transactional
class ListVersionMapperTest {
  @Autowired
  private ListVersionMapper listVersionMapper;

  @Test
  void shouldMapListVersionToDto() {
    ListVersion listVersion = TestDataFixture.getListVersion();
    ListVersionDTO dto = listVersionMapper.toListVersionDTO(listVersion);
    assertEquals(dto.getId(), listVersion.getId());
    assertEquals(dto.getListId(), listVersion.getListId());
    assertEquals(dto.getName(), listVersion.getName());
    assertEquals(dto.getFqlQuery(), listVersion.getFqlQuery());
    assertEquals(dto.getIsActive(), listVersion.getIsActive());
    assertEquals(dto.getVersion(), listVersion.getVersion());
    assertEquals(dto.getDescription(), listVersion.getDescription());
    assertEquals(dto.getUpdatedBy(), listVersion.getUpdatedBy());
    assertEquals(dto.getUpdatedByUsername(), listVersion.getUpdatedByUsername());
  }
}
