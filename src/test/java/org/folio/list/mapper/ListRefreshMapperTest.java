package org.folio.list.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.list.domain.ListRefreshDetails;
import org.folio.list.domain.dto.ListRefreshDTO;
import org.folio.list.util.TestDataFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = { ListRefreshMapperImpl.class })
class ListRefreshMapperTest {

  @Autowired
  private ListRefreshMapper refreshMapper;

  @Test
  void shouldMapEntityToDTO() {
    ListRefreshDetails listRefreshDetails = TestDataFixture.getListRefreshDetails();
    ListRefreshDTO dto = refreshMapper.toListRefreshDTO(listRefreshDetails);
    assertEquals(dto.getId(), listRefreshDetails.getId());
    assertEquals(dto.getListId(), listRefreshDetails.getListId());
    assertEquals(dto.getStatus().toString(), listRefreshDetails.getStatus().toString());
    assertEquals(dto.getRefreshStartDate(), listRefreshDetails.getRefreshStartDate());
    assertEquals(dto.getRefreshEndDate(), listRefreshDetails.getRefreshEndDate());
    assertEquals(dto.getRefreshedBy(), listRefreshDetails.getRefreshedBy());
    assertEquals(dto.getRefreshedByUsername(), listRefreshDetails.getRefreshedByUsername());
    assertEquals(dto.getListVersion(), listRefreshDetails.getListVersion());
    assertEquals(dto.getRecordsCount(), listRefreshDetails.getRecordsCount());
  }
}
