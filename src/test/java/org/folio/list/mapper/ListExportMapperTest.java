package org.folio.list.mapper;

import org.folio.list.domain.ExportDetails;
import org.folio.list.utils.TestDataFixture;
import org.folio.list.domain.dto.ListExportDTO;
import org.folio.list.mapper.ListExportMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {ListExportMapperImpl.class, MappingMethods.class})
class ListExportMapperTest {

  @Autowired
  private ListExportMapper listExportMapper;

  @Autowired
  private MappingMethods mappingMethods;

  @Test
  void shouldMapExportDetailsToDto() {
    ExportDetails exportDetails = TestDataFixture.getListExportDetails();
    ListExportDTO dto = listExportMapper.toListExportDTO(exportDetails);

    assertEquals(dto.getExportId(), exportDetails.getExportId());
    assertEquals(dto.getListId(), exportDetails.getList().getId());
    assertEquals(dto.getStatus().toString(), exportDetails.getStatus().toString());
    assertEquals(dto.getCreatedBy(), exportDetails.getCreatedBy());
    assertEquals(dto.getStartDate(), mappingMethods.offsetDateTimeAsDate(exportDetails.getStartDate()));
    assertEquals(dto.getEndDate(), mappingMethods.offsetDateTimeAsDate(exportDetails.getEndDate()));
  }
}
