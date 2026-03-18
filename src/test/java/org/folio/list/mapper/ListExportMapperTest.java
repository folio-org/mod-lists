package org.folio.list.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.list.domain.ExportDetails;
import org.folio.list.domain.dto.ListExportDTO;
import org.folio.list.util.TestDataFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = { ListExportMapperImpl.class })
class ListExportMapperTest {

  @Autowired
  private ListExportMapper listExportMapper;

  @Test
  void shouldMapExportDetailsToDto() {
    ExportDetails exportDetails = TestDataFixture.getListExportDetails();
    ListExportDTO dto = listExportMapper.toListExportDTO(exportDetails);

    assertEquals(dto.getExportId(), exportDetails.getExportId());
    assertEquals(dto.getListId(), exportDetails.getList().getId());
    assertEquals(dto.getStatus().toString(), exportDetails.getStatus().toString());
    assertEquals(dto.getCreatedBy(), exportDetails.getCreatedBy());
    assertEquals(dto.getStartDate(), exportDetails.getStartDate());
    assertEquals(dto.getEndDate(), exportDetails.getEndDate());
  }
}
