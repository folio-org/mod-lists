package org.folio.list.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.list.util.TestDataFixture;
import org.junit.jupiter.api.Test;

class ListVersionTest {

  @Test
  void shouldSetDataFromListEntity() {
    ListEntity listEntity = TestDataFixture.getListEntityWithSuccessRefresh();
    ListVersion listVersion = TestDataFixture.getListVersion();
    listVersion.setDataFromListEntity(listEntity);
    assertEquals(listVersion.getListId(), listEntity.getId());
    assertEquals(listVersion.getName(), listEntity.getName());
    assertEquals(listVersion.getFqlQuery(), listEntity.getFqlQuery());
    assertEquals(listVersion.getIsActive(), listEntity.getIsActive());
    assertEquals(listVersion.getUpdatedBy(), listEntity.getUpdatedBy());
    assertEquals(
      listVersion.getUpdatedByUsername(),
      listEntity.getUpdatedByUsername()
    );
  }
}
