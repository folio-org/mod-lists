package org.folio.list.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import org.folio.list.domain.AsyncProcessStatus;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.ListRefreshDetails;
import org.folio.list.repository.ListRepository;
import org.folio.list.services.ListService;
import org.folio.list.services.ListValidationService;
import org.folio.list.utils.TestDataFixture;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListServiceCancelRefreshTest {

  @InjectMocks
  private ListService listService;

  @Mock
  private ListRepository listRepository;

  @Mock
  private ListValidationService listValidationService;

  @Mock
  private FolioExecutionContext executionContext;

  @Test
  void shouldCancelListRefresh() {
    UUID userId = UUID.randomUUID();
    ListEntity list = TestDataFixture.getListEntityWithInProgressRefresh();
    ListRefreshDetails refreshDetails = list.getInProgressRefresh();
    when(listRepository.findByIdAndIsDeletedFalse(list.getId()))
      .thenReturn(Optional.of(list));
    when(executionContext.getUserId()).thenReturn(userId);
    doNothing().when(listValidationService).validateCancelRefresh(list);
    listService.cancelRefresh(list.getId());
    assertEquals(AsyncProcessStatus.CANCELLED, refreshDetails.getStatus());
    assertEquals(userId, refreshDetails.getCancelledBy());
    assertNull(list.getInProgressRefresh());
  }
}
