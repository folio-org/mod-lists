package org.folio.list.service.refresh;

import org.folio.list.domain.ListEntity;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListRepository;
import org.folio.list.services.refresh.RefreshFailedCallback;
import org.folio.list.util.TaskTimer;
import org.folio.list.utils.TestDataFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshFailedCallbackTest {
  @Mock
  private ListRepository listRepository;

  @Mock
  private ListContentsRepository listContentsRepository;

  @InjectMocks
  private RefreshFailedCallback failedRefreshService;

  @Test
  void shouldCallRefreshFailed() {
    ListEntity entity = TestDataFixture.getListEntityWithInProgressRefresh();
    Throwable failureReason = new RuntimeException("Something Went Wrong");
    String expectedErrorCode = "unexpected.error";
    when(listRepository.findByIdAndIsDeletedFalse(entity.getId())).thenReturn(Optional.of(entity));

    failedRefreshService.accept(entity, new TaskTimer(), failureReason);
    assertThat(entity.getFailedRefresh().getErrorMessage()).isEqualTo(failureReason.getMessage());
    assertThat(entity.getFailedRefresh().getErrorCode()).isEqualTo(expectedErrorCode);
    verify(listRepository, times(1)).save(entity);
    verify(listContentsRepository, times(1)).deleteContents(entity.getId(), entity.getFailedRefresh().getId());
  }
}
