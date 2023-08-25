package org.folio.list.service.refresh;

import org.folio.list.domain.AsyncProcessStatus;
import org.folio.list.domain.ListContent;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.ListRefreshDetails;
import org.folio.list.domain.dto.ListConfiguration;
import org.folio.list.exception.ListNotRefreshingException;
import org.folio.list.exception.MaxListSizeExceededException;
import org.folio.list.exception.RefreshCancelledException;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListRefreshRepository;
import org.folio.list.services.ListActions;
import org.folio.list.services.refresh.DataBatchCallback;
import org.folio.list.utils.TestDataFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataBatchCallbackTest {
  @Mock
  private ListRefreshRepository listRefreshRepository;

  @Mock
  private ListContentsRepository listContentsRepository;

  @Mock
  private ListConfiguration listConfiguration;

  @InjectMocks
  private DataBatchCallback dataBatchCallback;

  @Test
  void shouldProcessBatch() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    List<UUID> contentIds = List.of(id1, id2);
    ListEntity entity = TestDataFixture.getListEntityWithInProgressRefresh();
    ArgumentCaptor<List<ListContent>> savedContentsCapture = ArgumentCaptor.forClass(List.class);
    when(listConfiguration.getMaxListSize()).thenReturn(1250000);

    dataBatchCallback.accept(entity, contentIds);
    verify(listContentsRepository, times(1)).saveAll(savedContentsCapture.capture());
    List<ListContent> capturedContents = savedContentsCapture.getValue();

    assertThat(capturedContents.size()).isEqualTo(contentIds.size());
    assertThat(capturedContents.get(0).getListId()).isEqualTo(entity.getId());
    assertThat(capturedContents.get(0).getContentId()).isEqualTo(contentIds.get(0));
    assertThat(capturedContents.get(0).getRefreshId()).isEqualTo(entity.getInProgressRefreshId().orElseThrow(() -> new ListNotRefreshingException(entity, ListActions.REFRESH)));
    assertThat(capturedContents.get(0).getSortSequence()).isZero();

    assertThat(capturedContents.get(1).getListId()).isEqualTo(entity.getId());
    assertThat(capturedContents.get(1).getContentId()).isEqualTo(contentIds.get(1));
    assertThat(capturedContents.get(1).getRefreshId()).isEqualTo(entity.getInProgressRefreshId().orElseThrow(() -> new ListNotRefreshingException(entity, ListActions.REFRESH)));
    assertThat(capturedContents.get(1).getSortSequence()).isEqualTo(1);
  }

  @Test
  void shouldThrowExceptionWhenMaxListSizeIsExceeded() {
    List<UUID> contentIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    ListEntity entity = TestDataFixture.getListEntityWithInProgressRefresh();
    when(listConfiguration.getMaxListSize()).thenReturn(1);
    assertThrows(MaxListSizeExceededException.class, () -> dataBatchCallback.accept(entity, contentIds));
  }

  @Test
  void shouldThrowExceptionWhenRefreshIsCancelled() {
    List<UUID> contentIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    ListEntity entity = TestDataFixture.getListEntityWithInProgressRefresh();
    ListRefreshDetails refreshDetails = entity.getInProgressRefresh();
    refreshDetails.setStatus(AsyncProcessStatus.CANCELLED);
    when(listRefreshRepository.findById(refreshDetails.getId())).thenReturn(Optional.of(refreshDetails));
    assertThrows(RefreshCancelledException.class, () -> dataBatchCallback.accept(entity, contentIds));
  }
}
