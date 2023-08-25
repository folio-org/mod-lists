package org.folio.list.mapper;

import org.folio.list.domain.dto.ListRefreshDTO;
import org.folio.list.domain.ListRefreshDetails;
import org.folio.list.utils.TestDataFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {ListRefreshMapperImpl.class, MappingMethods.class})
class ListRefreshMapperTest {
  @Autowired
  private MappingMethods mappingMethods;

  @Autowired
  private ListRefreshMapper refreshMapper;

  @Test
  void shouldMapEntityToDTO() {
    ListRefreshDetails listRefreshDetails = TestDataFixture.getListRefreshDetails();
    ListRefreshDTO dto = refreshMapper.toListRefreshDTO(listRefreshDetails);
    assertEquals(dto.getId(), listRefreshDetails.getId());
    assertEquals(dto.getListId(), listRefreshDetails.getListId());
    assertEquals(dto.getStatus().toString(), listRefreshDetails.getStatus().toString());
    assertEquals(dto.getRefreshStartDate(), mappingMethods.offsetDateTimeAsDate(
      listRefreshDetails.getRefreshStartDate()));
    assertEquals(dto.getRefreshEndDate(), mappingMethods.offsetDateTimeAsDate(
      listRefreshDetails.getRefreshEndDate()));
    assertEquals(dto.getRefreshedBy(), listRefreshDetails.getRefreshedBy());
    assertEquals(dto.getRefreshedByUsername(), listRefreshDetails.getRefreshedByUsername());
    assertEquals(dto.getRecordsCount(), listRefreshDetails.getRecordsCount());
  }
}
