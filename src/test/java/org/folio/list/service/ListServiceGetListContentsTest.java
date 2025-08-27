package org.folio.list.service;

import feign.FeignException;
import org.folio.fql.service.FqlService;
import org.folio.list.domain.ListContent;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.ListRefreshDetails;
import org.folio.list.exception.ListContentsFqmRequestException;
import org.folio.list.exception.PrivateListOfAnotherUserException;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListRepository;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.rest.QueryClient;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListService;
import org.folio.list.services.ListValidationService;
import org.folio.list.util.TestDataFixture;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    List<EntityTypeColumn> columns = List.of(
      new EntityTypeColumn().name("id").isIdColumn(true).visibleByDefault(true),
      new EntityTypeColumn().name("something-else").visibleByDefault(false)
    );
    EntityType entityType = new EntityType().name("entity-type").columns(columns);
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
    when(entityTypeClient.getEntityType(entityTypeId, ListActions.READ)).thenReturn(entityType);
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
      new EntityTypeColumn().name("id").isIdColumn(true).visibleByDefault(true),
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

  @Test
  void shouldThrowInvalidListContentsExceptionWhenContentIdSizeMismatch() {
    String tenantId = "tenant_01";
    UUID entityTypeId = UUID.randomUUID();
    UUID listId = UUID.randomUUID();

    // Create content IDs with wrong size (2 IDs instead of expected 1)
    List<List<String>> contentIds = List.of(
      List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString())
    );

    // Entity type expects only 1 ID column
    List<EntityTypeColumn> columns = List.of(
      new EntityTypeColumn()
        .name("id")
        .isIdColumn(true)
        .visibleByDefault(true)
    );
    EntityType entityType = new EntityType().name("entity-type").columns(columns);

    int offset = 0;
    int size = 1;
    int contentVersion = 2;
    List<String> fields = List.of("id");

    List<ListContent> listContents = contentIds.stream()
      .map(id -> new ListContent().withContentId(id))
      .toList();

    ListRefreshDetails successRefresh = ListRefreshDetails.builder().contentVersion(contentVersion).recordsCount(1).build();
    ListEntity listEntity = new ListEntity()
      .withSuccessRefresh(successRefresh)
      .withEntityTypeId(entityTypeId)
      .withId(listId)
      .withFields(fields);

    when(executionContext.getTenantId()).thenReturn(tenantId);
    when(listRepository.findByIdAndIsDeletedFalse(listId)).thenReturn(Optional.of(listEntity));
    when(listContentsRepository.getContents(listId, successRefresh.getId(), new OffsetRequest(offset, size))).thenReturn(listContents);
    when(entityTypeClient.getEntityType(entityTypeId, ListActions.READ)).thenReturn(entityType);

    ListContentsFqmRequestException exception = assertThrows(
      ListContentsFqmRequestException.class,
      () -> listService.getListContents(listId, fields, offset, size)
    );

    assertThat(exception.getMessage()).contains("Please refresh the list");
  }

  @Test
  void shouldThrowInvalidListContentsExceptionWhenQueryClientThrowsFeignServerException() {
    String tenantId = "tenant_01";
    UUID entityTypeId = UUID.randomUUID();
    UUID listId = UUID.randomUUID();
    List<List<String>> contentIds = List.of(
      List.of(UUID.randomUUID().toString())
    );

    List<EntityTypeColumn> columns = List.of(
      new EntityTypeColumn().name("id").isIdColumn(true).visibleByDefault(true)
    );
    EntityType entityType = new EntityType().name("entity-type").columns(columns);

    int offset = 0;
    int size = 1;
    int contentVersion = 2;
    List<String> fields = List.of("id");
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(entityTypeId)
      .fields(fields)
      .ids(contentIds);

    List<ListContent> listContents = contentIds.stream()
      .map(id -> new ListContent().withContentId(id))
      .toList();

    ListRefreshDetails successRefresh = ListRefreshDetails.builder().contentVersion(contentVersion).recordsCount(1).build();
    ListEntity listEntity = new ListEntity()
      .withSuccessRefresh(successRefresh)
      .withEntityTypeId(entityTypeId)
      .withId(listId)
      .withFields(fields);

    // Mock a FeignServerException to be thrown by queryClient
    FeignException.FeignServerException feignException = mock(FeignException.FeignServerException.class);

    when(executionContext.getTenantId()).thenReturn(tenantId);
    when(listRepository.findByIdAndIsDeletedFalse(listId)).thenReturn(Optional.of(listEntity));
    when(listContentsRepository.getContents(listId, successRefresh.getId(), new OffsetRequest(offset, size))).thenReturn(listContents);
    when(entityTypeClient.getEntityType(entityTypeId, ListActions.READ)).thenReturn(entityType);
    when(queryClient.getContents(contentsRequest)).thenThrow(feignException);

    ListContentsFqmRequestException exception = assertThrows(
      ListContentsFqmRequestException.class,
      () -> listService.getListContents(listId, fields, offset, size)
    );

    assertThat(exception.getMessage()).contains("Failed to retrieve list contents");
  }

  @Test
  void shouldHandleEmptyContentIdsCorrectlyWithMultipleIdColumns() {
    String tenantId = "tenant_01";
    UUID entityTypeId = UUID.randomUUID();
    UUID listId = UUID.randomUUID();

    // Empty content IDs list
    List<List<String>> contentIds = List.of();

    // Entity type with multiple ID columns
    List<EntityTypeColumn> columns = List.of(
      new EntityTypeColumn().name("id").isIdColumn(true).visibleByDefault(true),
      new EntityTypeColumn().name("secondary_id").isIdColumn(true).visibleByDefault(true)
    );
    EntityType entityType = new EntityType().name("entity-type").columns(columns);

    int offset = 0;
    int size = 1;
    int contentVersion = 2;
    List<String> fields = List.of("id", "secondary_id");
    ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(entityTypeId)
      .fields(fields)
      .ids(contentIds);

    List<ListContent> listContents = List.of(); // Empty list contents

    ListRefreshDetails successRefresh = ListRefreshDetails.builder().contentVersion(contentVersion).recordsCount(0).build();
    ListEntity listEntity = new ListEntity()
      .withSuccessRefresh(successRefresh)
      .withEntityTypeId(entityTypeId)
      .withId(listId)
      .withFields(fields);

    when(executionContext.getTenantId()).thenReturn(tenantId);
    when(listRepository.findByIdAndIsDeletedFalse(listId)).thenReturn(Optional.of(listEntity));
    when(listContentsRepository.getContents(listId, successRefresh.getId(), new OffsetRequest(offset, size))).thenReturn(listContents);
    when(entityTypeClient.getEntityType(entityTypeId, ListActions.READ)).thenReturn(entityType);
    when(queryClient.getContents(contentsRequest)).thenReturn(List.of());

    Optional<ResultsetPage> result = listService.getListContents(listId, fields, offset, size);

    assertThat(result).isPresent();
    assertThat(result.get().getContent()).isEmpty();
    assertThat(result.get().getTotalRecords()).isZero();
  }

  @Test
  void shouldValidateContentIdSizeAgainstMultipleIdColumns() {
    String tenantId = "tenant_01";
    UUID entityTypeId = UUID.randomUUID();
    UUID listId = UUID.randomUUID();

    // Content IDs with only 1 ID but entity type expects 2
    List<List<String>> contentIds = List.of(
      List.of(UUID.randomUUID().toString()) // Only 1 ID
    );

    // Entity type expects 2 ID columns
    List<EntityTypeColumn> columns = List.of(
      new EntityTypeColumn().name("id").isIdColumn(true).visibleByDefault(true),
      new EntityTypeColumn().name("secondary_id").isIdColumn(true).visibleByDefault(true)
    );
    EntityType entityType = new EntityType().name("entity-type").columns(columns);

    int offset = 0;
    int size = 1;
    int contentVersion = 2;
    List<String> fields = List.of("id", "secondary_id");

    List<ListContent> listContents = contentIds.stream()
      .map(id -> new ListContent().withContentId(id))
      .toList();

    ListRefreshDetails successRefresh = ListRefreshDetails.builder().contentVersion(contentVersion).recordsCount(1).build();
    ListEntity listEntity = new ListEntity()
      .withSuccessRefresh(successRefresh)
      .withEntityTypeId(entityTypeId)
      .withId(listId)
      .withFields(fields);

    when(executionContext.getTenantId()).thenReturn(tenantId);
    when(listRepository.findByIdAndIsDeletedFalse(listId)).thenReturn(Optional.of(listEntity));
    when(listContentsRepository.getContents(listId, successRefresh.getId(), new OffsetRequest(offset, size))).thenReturn(listContents);
    when(entityTypeClient.getEntityType(entityTypeId, ListActions.READ)).thenReturn(entityType);

    ListContentsFqmRequestException exception = assertThrows(
      ListContentsFqmRequestException.class,
      () -> listService.getListContents(listId, fields, offset, size)
    );

    assertThat(exception.getMessage()).contains("Failed to retrieve list contents");
  }
}
