package org.folio.list.service;

import org.folio.fql.service.FqlService;
import org.folio.list.domain.ListContent;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.ListRefreshDetails;
import org.folio.list.exception.PrivateListOfAnotherUserException;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListRepository;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.rest.QueryClient;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListService;
import org.folio.list.services.ListValidationService;
import org.folio.list.utils.TestDataFixture;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.querytool.domain.dto.ResultsetPage;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListServiceGetListContentsTest {
  @Mock
  private FolioExecutionContext executionContext;
  @Mock
  private ListValidationService listValidationService;
  @Mock
  private ListRepository listRepository;
  @Mock
  private ListContentsRepository listContentsRepository;
  @Mock
  private FqlService fqlService;
  @Mock
  private QueryClient queryClient;
  @Mock
  private EntityTypeClient entityTypeClient;
  @InjectMocks
  private ListService listService;

  @Test
  void shouldReturnValidContentPage() {
    String tenantId = "tenant_01";
    UUID entityTypeId = UUID.randomUUID();
    UUID listId = UUID.randomUUID();
    List<List<String>> contentIds = List.of(
      List.of(UUID.randomUUID().toString()),
      List.of(UUID.randomUUID().toString())
    );
    int offset = 0;
    int size = 2;
    int contentVersion = 2;
    List<String> fields = List.of("id", "key1", "key2");
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(entityTypeId)
      .fields(fields)
      .ids(contentIds);
    List<Map<String, Object>> expectedList = List.of(
      Map.of("id", contentIds.get(0), "key1", "value1", "key2", "value2"),
      Map.of("id", contentIds.get(1), "key1", "value3", "key2", "value4")
    );
    Optional<ResultsetPage> expectedContent = Optional.of(new ResultsetPage().content(expectedList).totalRecords(expectedList.size()));

    List<ListContent> listContents = contentIds.stream().map(id -> {
      ListContent content = new ListContent();
      content.setContentId(id);
      return content;
    }).toList();

    ListEntity expectedEntity = new ListEntity();
    ListRefreshDetails successRefresh = new ListRefreshDetails();
    successRefresh.setContentVersion(contentVersion);
    expectedEntity.setSuccessRefresh(successRefresh);
    expectedEntity.setEntityTypeId(entityTypeId);
    expectedEntity.setId(listId);
    expectedEntity.setFields(fields);
    expectedEntity.getSuccessRefresh().setRecordsCount(2);

    when(executionContext.getTenantId()).thenReturn(tenantId);
    when(listRepository.findByIdAndIsDeletedFalse(listId)).thenReturn(Optional.of(expectedEntity));
    when(listContentsRepository.getContents(listId, successRefresh.getId(), new OffsetRequest(offset, size))).thenReturn(listContents);
    when(queryClient.getContents(contentsRequest)).thenReturn(expectedList);
    Optional<ResultsetPage> actualContent = listService.getListContents(listId, fields, offset, size);
    assertThat(actualContent).isEqualTo(expectedContent);
  }

  @Test
  void shouldGetFieldsFromEntityTypeIfNotProvided() {
    String tenantId = "tenant_01";
    UUID entityTypeId = UUID.randomUUID();
    UUID listId = UUID.randomUUID();
    List<List<String>> contentIds = List.of(
      List.of(UUID.randomUUID().toString()),
      List.of(UUID.randomUUID().toString())
    );
    List<EntityTypeColumn> columns = List.of(
      new EntityTypeColumn().name("id").visibleByDefault(true),
      new EntityTypeColumn().name("something-else").visibleByDefault(false)
    );
    EntityType entityType = new EntityType().name("entity-type").columns(columns);
    int offset = 0;
    int size = 2;
    int contentVersion = 2;
    List<String> fields = List.of();
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(entityTypeId)
      .fields(List.of("id", "something-else"))
      .ids(contentIds);
    List<Map<String, Object>> expectedList = List.of(
      Map.of("id", contentIds.get(0)),
      Map.of("id", contentIds.get(1))
    );
    Optional<ResultsetPage> expectedContent = Optional.of(new ResultsetPage().content(expectedList).totalRecords(expectedList.size()));

    List<ListContent> listContents = contentIds.stream().map(id -> {
      ListContent content = new ListContent();
      content.setContentId(id);
      return content;
    }).toList();

    ListEntity expectedEntity = new ListEntity();
    ListRefreshDetails successRefresh = new ListRefreshDetails();
    successRefresh.setContentVersion(contentVersion);
    expectedEntity.setSuccessRefresh(successRefresh);
    expectedEntity.setEntityTypeId(entityTypeId);
    expectedEntity.setId(listId);
    expectedEntity.setFields(fields);
    expectedEntity.getSuccessRefresh().setRecordsCount(2);

    when(executionContext.getTenantId()).thenReturn(tenantId);
    when(entityTypeClient.getEntityType(entityTypeId, ListActions.READ)).thenReturn(entityType);
    when(listRepository.findByIdAndIsDeletedFalse(listId)).thenReturn(Optional.of(expectedEntity));
    when(listContentsRepository.getContents(listId, successRefresh.getId(), new OffsetRequest(offset, size))).thenReturn(listContents);
    when(queryClient.getContents(contentsRequest)).thenReturn(expectedList);
    Optional<ResultsetPage> actualContent = listService.getListContents(listId, fields, offset, size);
    assertThat(actualContent).isEqualTo(expectedContent);
  }

  @Test
  void shouldReturnEmptyContentPageIfNotRefreshed() {
    UUID listId = UUID.randomUUID();
    List<String> fields = List.of("id", "key1", "key2");
    Optional<ResultsetPage> emptyContent = Optional.of(new ResultsetPage().content(List.of()).totalRecords(0));
    ListEntity neverRefreshedList = TestDataFixture.getNeverRefreshedListEntity();
    when(listRepository.findByIdAndIsDeletedFalse(listId)).thenReturn(Optional.of(neverRefreshedList));
    Optional<ResultsetPage> actualContent = listService.getListContents(listId, fields, 0, 100);
    assertThat(actualContent).isEqualTo(emptyContent);
  }

  @Test
  void shouldThrowExceptionWhenValidationFailed() {
    UUID listId = UUID.randomUUID();
    ListEntity listEntity = TestDataFixture.getNeverRefreshedListEntity();
    List<String> fields = List.of("id", "key1", "key2");
    when(listRepository.findByIdAndIsDeletedFalse(listId)).thenReturn(Optional.of(listEntity));
    doThrow(new PrivateListOfAnotherUserException(listEntity, ListActions.READ))
      .when(listValidationService).validateRead(listEntity);
    Assertions.assertThrows(PrivateListOfAnotherUserException.class, () -> listService.getListContents(listId, fields, 0, 100));
  }
}
