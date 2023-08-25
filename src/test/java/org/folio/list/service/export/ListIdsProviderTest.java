package org.folio.list.service.export;

import org.folio.list.domain.ListContent;
import org.folio.list.domain.ListEntity;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.services.export.ListIdsProvider;
import org.folio.list.utils.TestDataFixture;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.folio.list.domain.ListContent.SORT_SEQUENCE_START_NUMBER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ListIdsProviderTest {

  @Test
  void shouldReturnIdsInBatch() {
    int batchSize = 3;
    ListContentsRepository repository = mock(ListContentsRepository.class);
    ListEntity list = TestDataFixture.getPrivateListEntity();
    UUID refreshId = UUID.randomUUID();
    ListIdsProvider listIdsProvider = new ListIdsProvider(repository, list);
    List<ListContent> batch1 = List.of(
      new ListContent(list.getId(), refreshId, UUID.randomUUID(), 0),
      new ListContent(list.getId(), refreshId, UUID.randomUUID(), 1),
      new ListContent(list.getId(), refreshId, UUID.randomUUID(), 2)
    );
    List<ListContent> batch2 = List.of(
      new ListContent(list.getId(), refreshId, UUID.randomUUID(), 3),
      new ListContent(list.getId(), refreshId, UUID.randomUUID(), 4)
    );

    when(repository.getContents(list.getId(), list.getSuccessRefresh().getId(), SORT_SEQUENCE_START_NUMBER - 1,
      Pageable.ofSize(batchSize))).thenReturn(batch1);
    when(repository.getContents(list.getId(), list.getSuccessRefresh().getId(), batch1.get(batch1.size() - 1).getSortSequence(),
      Pageable.ofSize(batchSize))).thenReturn(batch2);

    List<UUID> actualBatch1 = listIdsProvider.nextBatch(batchSize);
    List<UUID> actualBatch2 = listIdsProvider.nextBatch(batchSize);

    assertEquals(batch1.stream().map(ListContent::getContentId).toList(), actualBatch1);
    assertEquals(batch2.stream().map(ListContent::getContentId).toList(), actualBatch2);
  }
}
