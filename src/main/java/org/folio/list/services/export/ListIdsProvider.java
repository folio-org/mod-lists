package org.folio.list.services.export;

import lombok.extern.log4j.Log4j2;
import org.folio.list.domain.ListContent;
import org.folio.list.domain.ListEntity;
import org.folio.list.repository.ListContentsRepository;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.folio.list.domain.ListContent.SORT_SEQUENCE_START_NUMBER;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Class responsible for providing the IDs of a list in batches
 */
@Log4j2
public class ListIdsProvider {
  private final ListContentsRepository repository;
  private final ListEntity list;
  private int previousSortSequence = SORT_SEQUENCE_START_NUMBER - 1;

  public ListIdsProvider(ListContentsRepository repository, ListEntity list) {
    this.repository = repository;
    this.list = list;
  }

  public List<UUID> nextBatch(int batchSize) {
    log.info("Fetching {} contents of list {} after sequence number {}", batchSize, list.getId(), previousSortSequence);

    List<ListContent> listContents = repository.getContents(list.getId(), list.getSuccessRefresh().getId(), previousSortSequence,
      PageRequest.ofSize(batchSize));

    if (isEmpty(listContents)) {
      return List.of();
    }

    previousSortSequence = listContents.stream()
      .mapToInt(ListContent::getSortSequence)
      .reduce((first, second) -> second)
      .orElse(Integer.MAX_VALUE); // should not be here

    return listContents
      .stream()
      .map(ListContent::getContentId)
      .toList();
  }
}
