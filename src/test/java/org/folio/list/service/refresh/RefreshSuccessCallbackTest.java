package org.folio.list.service.refresh;

import org.folio.list.domain.ListEntity;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListRepository;
import org.folio.list.services.EntityManagerFlushService;
import org.folio.list.services.refresh.RefreshSuccessCallback;
import org.folio.list.util.TaskTimer;
import org.folio.list.util.TestDataFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class RefreshSuccessCallbackTest {
  @Mock
  private ListRepository listRepository;

  @Mock
  private ListContentsRepository listContentsRepository;

  @Mock
  private EntityManagerFlushService entityManagerFlushService;

  @InjectMocks
  private RefreshSuccessCallback successRefreshService;

  @Test
  void shouldCompleteRefreshForNeverRefreshedList() {
    int recordsCount = 10;
    ListEntity entity = TestDataFixture.getListEntityWithInProgressRefresh();
    when(listRepository.findByIdAndIsDeletedFalse(entity.getId())).thenReturn(Optional.of(entity));

    successRefreshService.accept(entity, recordsCount, new TaskTimer());
    assertEquals(entity.getSuccessRefresh().getRecordsCount(), recordsCount);
    // Delete contents should not be called if list has never been refreshed
    verify(listContentsRepository, times(0)).deleteContents(any(), any());
    verify(listRepository, times(1)).save(entity);
  }

  @Test
  void shouldCompleteRefreshForPreviouslyRefreshedList() {
    int recordsCount = 10;
    ListEntity entity = TestDataFixture.getListEntityWithInProgressAndSuccessRefresh();
    UUID originalRefreshId = entity.getSuccessRefresh().getId();
    when(listRepository.findByIdAndIsDeletedFalse(entity.getId())).thenReturn(Optional.of(entity));

    successRefreshService.accept(entity, recordsCount, new TaskTimer());
    assertEquals(entity.getSuccessRefresh().getRecordsCount(), recordsCount);
    verify(listContentsRepository, times(1)).deleteContents(entity.getId(), originalRefreshId);
    verify(listRepository, times(1)).save(entity);
  }
}
