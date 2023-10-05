package org.folio.list.service;

import org.folio.fql.FqlService;
import org.folio.fql.model.EqualsCondition;
import org.folio.fql.model.Fql;
import org.folio.fqm.lib.service.FqmMetaDataService;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.dto.ListDTO;
import org.folio.list.domain.dto.ListRequestDTO;
import org.folio.list.domain.dto.ListSummaryDTO;
import org.folio.list.domain.dto.ListUpdateRequestDTO;
import org.folio.list.mapper.*;
import org.folio.list.mapper.ListMapperImpl;
import org.folio.list.mapper.ListRefreshMapperImpl;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListRepository;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.rest.EntityTypeClient.EntityTypeSummary;
import org.folio.list.rest.QueryClient;
import org.folio.list.rest.UsersClient;
import org.folio.list.rest.UsersClient.User;
import org.folio.list.services.AppShutdownService;
import org.folio.list.services.UserFriendlyQueryService;
import org.folio.list.services.refresh.ListRefreshService;
import org.folio.list.services.ListService;
import org.folio.list.services.ListValidationService;
import org.folio.list.utils.TestDataFixture;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.util.StringUtils.hasText;

@ExtendWith(MockitoExtension.class)
class ListServiceTest {
  public static final String FIRSTNAME = "firstname";
  public static final String LASTNAME = "lastname";

  @InjectMocks
  private ListService listService;

  @Mock
  private ListRepository listRepository;

  @Mock
  private ListSummaryMapper listSummaryMapper;

  @Spy
  private ListMapper listMapper = new ListMapperImpl(new MappingMethods(), new ListRefreshMapperImpl(new MappingMethods()));

  @Spy
  private ListEntityMapper listEntityMapper = new org.folio.list.mapper.ListEntityMapperImpl();

  @Mock
  private FolioExecutionContext executionContext;

  @Mock
  private UsersClient usersClient;

  @Mock
  private EntityTypeClient entityTypeClient;

  @Mock
  private ListValidationService validationService;

  @Mock
  private ListRefreshService listRefreshService;

  @Mock
  private ListContentsRepository listContentsRepository;

  @Mock
  private UserFriendlyQueryService userFriendlyQueryService;

  @Mock
  private FqlService fqlService;

  @Mock
  private AppShutdownService appShutdownService;

  @Test
  void testGetAllLists() {
    UUID entityTypeId1 = UUID.randomUUID();
    UUID entityTypeId2 = UUID.randomUUID();
    UUID currentUserId = UUID.randomUUID();
    ListEntity entity1 = TestDataFixture.getListEntityWithSuccessRefresh(UUID.randomUUID());
    ListEntity entity2 = TestDataFixture.getListEntityWithSuccessRefresh(UUID.randomUUID());
    entity1.setEntityTypeId(entityTypeId1);
    entity2.setEntityTypeId(entityTypeId2);

    ListSummaryDTO listSummaryDto1 = TestDataFixture.getListSummaryDTO(entity1.getId()).entityTypeId(entityTypeId1);
    ListSummaryDTO listSummaryDto2 = TestDataFixture.getListSummaryDTO(entity2.getId()).entityTypeId(entityTypeId2);
    EntityTypeSummary expectedSummary1 = new EntityTypeClient.EntityTypeSummary(listSummaryDto1.getEntityTypeId(), "Item");
    EntityTypeSummary expectedSummary2 = new EntityTypeClient.EntityTypeSummary(listSummaryDto2.getEntityTypeId(), "Loan");

    Page<ListEntity> listEntities = new PageImpl<>(List.of(entity1, entity2));
    when(executionContext.getUserId()).thenReturn(currentUserId);
    when(listRepository.searchList(any(Pageable.class), Mockito.eq(List.of(entity1.getId(), entity2.getId())), Mockito.eq(List.of(entity1.getEntityTypeId(), entity2.getEntityTypeId())), any(UUID.class), Mockito.eq(true), Mockito.eq(false), any())
    ).thenReturn(listEntities);
    when(listSummaryMapper.toListSummaryDTO(entity1, "Item")).thenReturn(listSummaryDto1.entityTypeName("Item"));
    when(listSummaryMapper.toListSummaryDTO(entity2, "Loan")).thenReturn(listSummaryDto2.entityTypeName("Loan"));
    when(entityTypeClient.getEntityTypeSummary(List.of(listSummaryDto1.getEntityTypeId(),
      listSummaryDto2.getEntityTypeId()))).thenReturn(List.of(expectedSummary1, expectedSummary2));

    Page<ListSummaryDTO> expected = new PageImpl<>(List.of(listSummaryDto1, listSummaryDto2));

    var actual = listService.getAllLists(Pageable.ofSize(100),
      List.of(entity1.getId(), entity2.getId()),
      List.of(entity1.getEntityTypeId(), entity2.getEntityTypeId()),
      true,
      false,
      null);
    assertThat(actual.getContent()).isEqualTo(expected.getContent());
  }

  @Test
  void testCreateList() {
    ListRequestDTO listRequestDto = TestDataFixture.getListRequestDTO();
    UUID userId = UUID.randomUUID();
    String userFriendlyQuery = "some string";
    User user = new User(userId, Optional.of(new UsersClient.Personal("firstname", "lastname")));
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh(UUID.randomUUID());
    ListDTO expected = TestDataFixture.getListDTOSuccessRefresh(userId);
    EntityType entityType = new EntityType().id(entity.getEntityTypeId().toString());
    EqualsCondition equalsCondition = new EqualsCondition("item_status", "missing");

    when(usersClient.getUser(userId)).thenReturn(user);
    when(executionContext.getUserId()).thenReturn(userId);
    when(listEntityMapper.toListEntity(listRequestDto, user)).thenReturn(entity);
    when(listRepository.save(entity)).thenReturn(entity);

    when(entityTypeClient.getEntityType(entity.getEntityTypeId())).thenReturn(entityType);
    when(fqlService.getFql(entity.getFqlQuery())).thenReturn(new Fql(equalsCondition));
    when(userFriendlyQueryService.getUserFriendlyQuery(equalsCondition, entityType)).thenReturn(userFriendlyQuery);
    when(listMapper.toListDTO(entity)).thenReturn(expected);

    var actual = listService.createList(listRequestDto);
    assertEquals(entity.getUserFriendlyQuery(), userFriendlyQuery);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void testCreateListWithoutFqlQuery() {
    ListRequestDTO listRequestDto = TestDataFixture.getListRequestDTO();
    listRequestDto.setFqlQuery("");
    EntityType entityType = new EntityType().name("test-entity");
    UUID userId = UUID.randomUUID();
    User user = new User(userId, Optional.of(new UsersClient.Personal("firstname", "lastname")));
    ArgumentCaptor<ListEntity> listEntityArgumentCaptor = ArgumentCaptor.forClass(ListEntity.class);

    when(usersClient.getUser(userId)).thenReturn(user);
    when(executionContext.getUserId()).thenReturn(userId);
    when(entityTypeClient.getEntityType(listRequestDto.getEntityTypeId())).thenReturn(entityType);

    listService.createList(listRequestDto);

    verify(listRepository, times(1)).save(listEntityArgumentCaptor.capture());
    ListEntity list = listEntityArgumentCaptor.getValue();
    assertFalse(hasText(list.getUserFriendlyQuery()));
  }

  @Test
  void testCreateListFromQueryId() {
    UUID queryId = UUID.randomUUID();
    ListRequestDTO listRequestDto = TestDataFixture.getListRequestDTO().queryId(queryId);
    UUID userId = UUID.randomUUID();
    User user = new User(userId, Optional.of(new UsersClient.Personal("firstname", "lastname")));
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh(UUID.randomUUID());
    ListDTO expected = TestDataFixture.getListDTOSuccessRefresh(userId);
    EntityType entityType = new EntityType().id(entity.getEntityTypeId().toString());

    when(usersClient.getUser(userId)).thenReturn(user);
    when(executionContext.getUserId()).thenReturn(userId);
    when(entityTypeClient.getEntityType(listRequestDto.getEntityTypeId())).thenReturn(entityType);
    when(fqlService.getFql(entity.getFqlQuery())).thenReturn(new Fql(new EqualsCondition("item_status", "missing")));
    when(listEntityMapper.toListEntity(listRequestDto, user)).thenReturn(entity);
    when(listRepository.save(entity)).thenReturn(entity);
    when(listMapper.toListDTO(entity)).thenReturn(expected);

    var actual = listService.createList(listRequestDto);
    assertThat(actual).isEqualTo(expected);
    verify(listRefreshService, times(1)).doAsyncSorting(entity, queryId, null);
    verify(appShutdownService, times(1)).registerShutdownTask(eq(executionContext), any(Runnable.class), any(String.class));
  }

  @Test
  void testCreateInactiveListShouldNotRefresh() {
    ListRequestDTO listRequestDto = TestDataFixture.getListRequestDTO();
    listRequestDto.setIsActive(false);

    UUID userId = UUID.randomUUID();
    UUID queryId = UUID.randomUUID();
    String userFriendlyQuery = "some string";

    User user = new User(userId, Optional.of(new UsersClient.Personal("firstname", "lastname")));
    ListEntity entity = TestDataFixture.getInactiveListEntity();

    ListDTO expected = TestDataFixture.getListDTOSuccessRefresh(userId);
    listRequestDto.setQueryId(queryId);

    EntityType entityType = new EntityType().id(entity.getEntityTypeId().toString());
    EqualsCondition equalsCondition = new EqualsCondition("item_status", "missing");

    when(usersClient.getUser(userId)).thenReturn(user);
    when(executionContext.getUserId()).thenReturn(userId);
    when(listEntityMapper.toListEntity(listRequestDto, user)).thenReturn(entity);
    when(listRepository.save(entity)).thenReturn(entity);

    when(entityTypeClient.getEntityType(entity.getEntityTypeId())).thenReturn(entityType);
    when(fqlService.getFql(entity.getFqlQuery())).thenReturn(new Fql(equalsCondition));
    when(userFriendlyQueryService.getUserFriendlyQuery(equalsCondition, entityType)).thenReturn(userFriendlyQuery);
    when(listMapper.toListDTO(entity)).thenReturn(expected);
    listService.createList(listRequestDto);
    assertEquals(entity.getUserFriendlyQuery(), userFriendlyQuery);
    assertNull(entity.getSuccessRefresh());
    assertNull(entity.getFailedRefresh());
    assertNull(entity.getInProgressRefresh());
  }

  // This tests the UI-fields workaround and keeps sonar happy until the UI is updated to send fields in the request.
  @Test
  void shouldCreateListWithDefaultFieldsIfFieldsNotProvided() {
    ListRequestDTO listRequestDto = TestDataFixture.getListRequestDTO();
    listRequestDto.setFqlQuery("");
    listRequestDto.setFields(null);
    List<String> expectedFields = List.of("column_01", "column_02");
    EntityType entityType = new EntityType().name("test-entity").columns(List.of(
      new EntityTypeColumn().name("column_01"),
      new EntityTypeColumn().name("column_02")
      ));
    UUID userId = UUID.randomUUID();
    User user = new User(userId, Optional.of(new UsersClient.Personal("firstname", "lastname")));
    ArgumentCaptor<ListEntity> listEntityArgumentCaptor = ArgumentCaptor.forClass(ListEntity.class);

    when(usersClient.getUser(userId)).thenReturn(user);
    when(executionContext.getUserId()).thenReturn(userId);
    when(entityTypeClient.getEntityType(listRequestDto.getEntityTypeId())).thenReturn(entityType);
    listService.createList(listRequestDto);

    verify(listRepository, times(1)).save(listEntityArgumentCaptor.capture());
    ListEntity list = listEntityArgumentCaptor.getValue();
    assertFalse(hasText(list.getUserFriendlyQuery()));
    assertEquals(expectedFields, list.getFields());
  }

  @Test
  void testUpdateListForExistingList() {
    ListUpdateRequestDTO listUpdateRequestDto = TestDataFixture.getListUpdateRequestDTO();
    UUID userId = UUID.randomUUID();
    UUID listId = UUID.randomUUID();
    String userFriendlyQuery = "some string";

    User user = new User(userId, Optional.of(new UsersClient.Personal(FIRSTNAME, LASTNAME)));
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh(listId);
    ListDTO expected = TestDataFixture.getListDTOSuccessRefresh(userId);
    EntityType entityType = new EntityType().id(entity.getEntityTypeId().toString());
    EqualsCondition equalsCondition = new EqualsCondition("item_status", "missing");

    when(usersClient.getUser(userId)).thenReturn(user);
    when(executionContext.getUserId()).thenReturn(userId);
    when(entityTypeClient.getEntityType(entity.getEntityTypeId())).thenReturn(entityType);
    when(fqlService.getFql(entity.getFqlQuery())).thenReturn(new Fql(equalsCondition));
    when(userFriendlyQueryService.getUserFriendlyQuery(equalsCondition, entityType)).thenReturn(userFriendlyQuery);
    when(listRepository.findById(listId)).thenReturn(Optional.of(entity));
    when(listMapper.toListDTO(entity)).thenReturn(expected);
    doNothing().when(validationService).validateUpdate(entity, listUpdateRequestDto, entityType);
    int previousVersion = entity.getVersion();

    var actual = listService.updateList(listId, listUpdateRequestDto);
    assertThat(actual).contains(expected);
    assertThat(entity.getId()).isEqualTo(listId);
    assertThat(entity.getName()).isEqualTo(listUpdateRequestDto.getName());
    assertThat(entity.getDescription()).isEqualTo(listUpdateRequestDto.getDescription());
    assertThat(entity.getFqlQuery()).isEqualTo(listUpdateRequestDto.getFqlQuery());
    assertThat(entity.getFields()).isEqualTo(listUpdateRequestDto.getFields());
    assertThat(entity.getIsActive()).isEqualTo(listUpdateRequestDto.getIsActive());
    assertThat(entity.getIsPrivate()).isEqualTo(listUpdateRequestDto.getIsPrivate());
    assertThat(entity.getUpdatedBy()).isEqualTo(userId);
    assertThat(user.getFullName()).contains(entity.getUpdatedByUsername());
    assertThat(entity.getVersion()).isEqualTo(previousVersion + 1);
    assertThat(entity.getUserFriendlyQuery()).isEqualTo(userFriendlyQuery);
  }

  // This test can be removed once the UI has been updated to allow fields to be sent in the list update request
  @Test
  void updateListShouldUseDefaultFieldsIfFieldsNotProvidedInRequest() {
    ListUpdateRequestDTO listUpdateRequestDto = TestDataFixture.getListUpdateRequestDTO();
    listUpdateRequestDto.setFields(null);
    UUID userId = UUID.randomUUID();
    UUID listId = UUID.randomUUID();
    String userFriendlyQuery = "some string";
    List<String> expectedFields = List.of("field1");

    User user = new User(userId, Optional.of(new UsersClient.Personal(FIRSTNAME, LASTNAME)));
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh(listId);
    ListDTO expected = TestDataFixture.getListDTOSuccessRefresh(userId);
    EntityType entityType = new EntityType()
      .id(entity.getEntityTypeId().toString())
      .columns(List.of(new EntityTypeColumn().name("field1")));
    EqualsCondition equalsCondition = new EqualsCondition("item_status", "missing");

    when(usersClient.getUser(userId)).thenReturn(user);
    when(executionContext.getUserId()).thenReturn(userId);
    when(entityTypeClient.getEntityType(entity.getEntityTypeId())).thenReturn(entityType);
    when(fqlService.getFql(entity.getFqlQuery())).thenReturn(new Fql(equalsCondition));
    when(userFriendlyQueryService.getUserFriendlyQuery(equalsCondition, entityType)).thenReturn(userFriendlyQuery);
    when(listRepository.findById(listId)).thenReturn(Optional.of(entity));
    when(listMapper.toListDTO(entity)).thenReturn(expected);
    doNothing().when(validationService).validateUpdate(entity, listUpdateRequestDto, entityType);

    listService.updateList(listId, listUpdateRequestDto);
    assertThat(entity.getId()).isEqualTo(listId);
    assertEquals(expectedFields, entity.getFields());
  }

  @Test
  void testDeactivateListShouldRemoveContents() {
    UUID userId = UUID.randomUUID();
    UUID listId = UUID.randomUUID();
    EntityType entityType = new EntityType().name("test-entity");
    User user = new User(userId, Optional.of(new UsersClient.Personal(FIRSTNAME, LASTNAME)));
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh(listId);
    var deactivateRequest = TestDataFixture.getListUpdateRequestDTO();
    deactivateRequest.setFqlQuery("");
    deactivateRequest.setIsActive(false);

    when(usersClient.getUser(userId)).thenReturn(user);
    when(executionContext.getUserId()).thenReturn(userId);
    when(entityTypeClient.getEntityType(entity.getEntityTypeId())).thenReturn(entityType);
    when(listRepository.findById(listId)).thenReturn(Optional.of(entity));
    doNothing().when(validationService).validateUpdate(entity, deactivateRequest, entityType);

    assertThat(entity.getIsActive())
      .withFailMessage("The list should be active before we deactivate it")
      .isTrue();
    assertThat(listService.updateList(listId, deactivateRequest))
      .withFailMessage("The output from the update call should be non-empty, indicating that it successfully found the list")
      .isPresent();
    assertThat(entity.getIsActive())
      .withFailMessage("isActive on the list object is modified by ListService, so we should see that change")
      .isFalse();
    verify(listContentsRepository, times(1)).deleteContents(listId);
  }

  @Test
  void testUpdateListFromQueryId() {
    EntityType entityType = new EntityType().name("test-entity");
    UUID queryId = UUID.randomUUID();
    ListUpdateRequestDTO listUpdateRequestDto = TestDataFixture.getListUpdateRequestDTO().queryId(queryId);
    UUID userId = UUID.randomUUID();
    listUpdateRequestDto.setFqlQuery("");

    User user = new User(userId, Optional.of(new UsersClient.Personal("firstname", "lastname")));
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh(UUID.randomUUID());

    when(usersClient.getUser(userId)).thenReturn(user);
    when(executionContext.getUserId()).thenReturn(userId);
    when(entityTypeClient.getEntityType(entity.getEntityTypeId())).thenReturn(entityType);
    when(listRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
    when(listRepository.save(entity)).thenReturn(entity);
    int oldVersion = entity.getVersion(); // Save the original version, since updateList modifies entity
    var actual = listService.updateList(entity.getId(), listUpdateRequestDto);

    assertThat(actual).map(ListDTO::getId).contains(entity.getId());
    assertThat(actual).map(ListDTO::getSuccessRefresh).isNotEmpty();
    assertThat(actual).map(ListDTO::getIsActive).contains(true);
    assertThat(actual).map(ListDTO::getVersion).contains(oldVersion + 1);
    verify(listRefreshService, times(1)).doAsyncSorting(entity, queryId, null);
  }

  @Test
  void testUpdateInactiveListFromQueryIdDoesNotImportQueryData() {
    EntityType entityType = new EntityType().name("test-entity");
    UUID queryId = UUID.randomUUID();
    ListUpdateRequestDTO listUpdateRequestDto = TestDataFixture.getListUpdateRequestDTO()
      .queryId(queryId)
      .isActive(false);
    listUpdateRequestDto.setFqlQuery("");
    UUID userId = UUID.randomUUID();

    User user = new User(userId, Optional.of(new UsersClient.Personal("firstname", "lastname")));
    ListEntity entity = TestDataFixture.getListEntityWithSuccessRefresh(UUID.randomUUID());
    ListDTO expected = listMapper.toListDTO(entity)
      .successRefresh(null)
      .version(entity.getVersion() + 1)
      .isActive(false);

    when(usersClient.getUser(userId)).thenReturn(user);
    when(executionContext.getUserId()).thenReturn(userId);
    when(listRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
    when(entityTypeClient.getEntityType(entity.getEntityTypeId())).thenReturn(entityType);

    int oldVersion = entity.getVersion(); // Save the original version, since updateList modifies entity
    var actual = listService.updateList(entity.getId(), listUpdateRequestDto);

    verify(listRefreshService, never()).doAsyncSorting(entity, queryId, null);
    assertThat(actual).map(ListDTO::getSuccessRefresh).isEmpty();
    assertThat(actual).map(ListDTO::getVersion).contains(oldVersion + 1);
    assertThat(actual).map(ListDTO::getIsActive).contains(expected.getIsActive());
  }
}
