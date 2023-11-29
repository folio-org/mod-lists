package org.folio.list.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.fql.service.FqlService;
import org.folio.fql.model.Fql;
import org.folio.list.domain.ListContent;
import org.folio.list.domain.ListRefreshDetails;
import org.folio.list.domain.dto.ListDTO;
import org.folio.list.domain.dto.ListRefreshDTO;
import org.folio.list.domain.dto.ListRequestDTO;
import org.folio.list.domain.dto.ListSummaryDTO;
import org.folio.list.domain.dto.ListSummaryResultsDTO;
import org.folio.list.domain.dto.ListUpdateRequestDTO;
import org.folio.list.exception.ListNotFoundException;
import org.folio.list.exception.RefreshInProgressDuringShutdownException;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.rest.QueryClient;
import org.folio.list.services.refresh.ListRefreshService;
import org.folio.list.services.refresh.RefreshFailedCallback;
import org.folio.list.services.refresh.TimedStage;
import org.folio.list.util.TaskTimer;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.ResultsetPage;
import org.folio.list.domain.ListEntity;
import org.folio.list.mapper.ListEntityMapper;
import org.folio.list.mapper.ListMapper;
import org.folio.list.mapper.ListRefreshMapper;
import org.folio.list.mapper.ListSummaryMapper;
import org.folio.list.repository.ListRepository;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.rest.EntityTypeClient.EntityTypeSummary;
import org.folio.list.rest.UsersClient;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.OffsetDateTime;
import java.util.*;

import static java.util.stream.Collectors.toMap;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class ListService {
  private final ListRepository listRepository;
  private final ListContentsRepository listContentsRepository;
  private final ListMapper listMapper;
  private final ListSummaryMapper summaryMapper;
  private final ListRefreshMapper refreshMapper;
  private final ListEntityMapper listEntityMapper;
  private final FolioExecutionContext executionContext;
  private final ListRefreshService listRefreshService;
  private final ListValidationService validationService;
  private final UsersClient usersClient;
  private final EntityTypeClient entityTypeClient;
  private final FqlService fqlService;
  private final UserFriendlyQueryService userFriendlyQueryService;
  private final AppShutdownService appShutdownService;
  private final RefreshFailedCallback refreshFailedCallback;
  private final QueryClient queryClient;

  public ListSummaryResultsDTO getAllLists(Pageable pageable, List<UUID> ids,
                                           List<UUID> entityTypeIds, Boolean active, Boolean isPrivate, OffsetDateTime updatedAsOf) {

    log.info("Attempting to get all lists");
    UUID currentUserId = executionContext.getUserId();
    Page<ListEntity> lists = listRepository.searchList(pageable, isEmpty(ids) ? null : ids,
      isEmpty(entityTypeIds) ? null : entityTypeIds, currentUserId, active, isPrivate, updatedAsOf);

    // List database do not store entity type labels. Only entity type ID is available in List database.
    // Get the corresponding entity type labels from FQM
    Map<UUID, String> entityTypeIdLabelPair = getEntityTypeLabels(lists.getContent());
    List<ListSummaryDTO> content = lists
      .map(l -> summaryMapper.toListSummaryDTO(l, entityTypeIdLabelPair.get(l.getEntityTypeId())))
      .getContent();
    return new ListSummaryResultsDTO()
      .content(content)
      .totalRecords(lists.getTotalElements())
      .totalPages(lists.getTotalPages());
  }

  public ListDTO createList(ListRequestDTO listRequest) {
    log.info("Attempting to create a list");
    EntityType entityType = getEntityType(listRequest.getEntityTypeId());
    validationService.validateCreate(listRequest, entityType);
    UsersClient.User currentUser = getCurrentUser();
    ListEntity listEntity = listEntityMapper.toListEntity(listRequest, currentUser);

    // Once UI has been updated to support sending fields in the request, the below if-block
    // can be removed
    if (CollectionUtils.isEmpty(listEntity.getFields())) {
      listEntity.setFields(getFieldsFromEntityType(entityType));
    }

    if (hasText(listRequest.getFqlQuery())) {
      String userFriendlyQuery = getUserFriendlyQuery(listRequest.getFqlQuery(), entityType);
      listEntity.setUserFriendlyQuery(userFriendlyQuery);
    }
    ListEntity savedEntity = listRepository.save(listEntity);
    if (nonNull(listRequest.getQueryId()) && listRequest.getIsActive()) {
      TaskTimer timer = new TaskTimer();
      timer.start(TimedStage.TOTAL);
      importListContentsFromAsyncQuery(savedEntity, currentUser, listRequest.getQueryId(), timer);
    }
    return listMapper.toListDTO(savedEntity);
  }

  public Optional<ListDTO> updateList(UUID id, ListUpdateRequestDTO request) {
    log.info("Attempting to update a list with id : {}", id);
    Optional<ListEntity> listEntity = listRepository.findById(id);
    listEntity.ifPresent(list -> {
      EntityType entityType = getEntityType(list.getEntityTypeId());
      validationService.validateUpdate(list, request, entityType);
      if (!request.getIsActive()) {
        // If we're deactivating a list, clear its contents and refresh data
        listContentsRepository.deleteContents(id);
        list.setSuccessRefresh(null);
      }
      // Once UI has been updated to support sending fields in the request, the below if-block
      // can be removed
      if (isEmpty(request.getFields())) {
        request.setFields(getFieldsFromEntityType(entityType));
      }
      String userFriendlyQuery = "";
      if (hasText(request.getFqlQuery())) {
        userFriendlyQuery = getUserFriendlyQuery(request.getFqlQuery(), entityType);
      }
      list.update(request, getCurrentUser(), userFriendlyQuery);

      if (request.getQueryId() != null && request.getIsActive()) {
        TaskTimer timer = new TaskTimer();
        timer.start(TimedStage.TOTAL);
        importListContentsFromAsyncQuery(list, getCurrentUser(), request.getQueryId(), timer);
      }
    });
    return listEntity.map(listMapper::toListDTO);
  }

  public Optional<ListDTO> getListById(UUID id) {
    log.info("Attempting to get specific list for id {}", id);
    return listRepository.findById(id)
      .map(list -> {
        validationService.assertSharedOrOwnedByUser(list, ListActions.READ);
        return listMapper.toListDTO(list);
      });
  }

  public Optional<ListRefreshDTO> performRefresh(UUID listId) {
    log.info("Attempting to refresh list with listId {}", listId);
    return listRepository.findById(listId)
      .map(list -> {
        validationService.validateRefresh(list);
        list.refreshStarted(getCurrentUser());
        TaskTimer timer = new TaskTimer();
        timer.start(TimedStage.TOTAL);
        ListEntity savedList = timer.time(TimedStage.WRITE_START, () -> listRepository.save(list));
        listRefreshService.doAsyncRefresh(
          savedList,
          registerShutdownTask(savedList, "Cancel refresh for list " + savedList.getId()),
          timer);
        ListRefreshDetails refreshDetails = savedList.getInProgressRefresh();
        return refreshMapper.toListRefreshDTO(refreshDetails);
      });
  }

  public Optional<ResultsetPage> getListContents(UUID listId, Integer offset, Integer size) {
    log.info("Attempting to get contents for list with listId {}, tenantId {}, offset {}, size {}",
      listId, executionContext.getTenantId(), offset, size);
    return listRepository.findById(listId)
      .map(list -> {
        validationService.assertSharedOrOwnedByUser(list, ListActions.READ);
        return getListContents(list, offset, size);
      });
  }

  public void deleteList(UUID id) {
    ListEntity list = listRepository.findById(id)
      .orElseThrow(() -> new ListNotFoundException(id, ListActions.DELETE));
    validationService.validateDelete(list);
    deleteListAndContents(list);
  }

  public void cancelRefresh(UUID listId) {
    log.info("Cancelling refresh for list {}", listId);
    ListEntity list = listRepository.findById(listId)
      .orElseThrow(() -> new ListNotFoundException(listId, ListActions.CANCEL_REFRESH));
    validationService.validateCancelRefresh(list);
    list.refreshCancelled(executionContext.getUserId());
  }

  private void deleteListAndContents(ListEntity list) {
    listContentsRepository.deleteContents(list.getId());
    listRepository.deleteById(list.getId());
  }

  private ResultsetPage getListContents(ListEntity list, Integer offset, Integer limit) {
    List<Map<String, Object>> sortedContents = List.of();
    List<String> fields = list.getFields();
    if (CollectionUtils.isEmpty(fields)) {
      Fql fql = fqlService.getFql(list.getFqlQuery());
      fields = fqlService.getFqlFields(fql);
    }
    if (!fields.contains("id")) {
      fields.add("id");
    }
    if (list.isRefreshed()) {
      List<UUID> contentIds = listContentsRepository.getContents(list.getId(), list.getSuccessRefresh().getId(), new OffsetRequest(offset, limit))
        .stream()
        .map(ListContent::getContentId)
        .toList();
      ContentsRequest contentsRequest = new ContentsRequest().entityTypeId(list.getEntityTypeId())
        .fields(fields)
        .ids(contentIds);
      sortedContents = queryClient.getContents(contentsRequest);
    }
    return new ResultsetPage().content(sortedContents).totalRecords(list.getRecordsCount());
  }

  private Map<UUID, String> getEntityTypeLabels(List<ListEntity> lists) {
    try {
      List<UUID> entityTypeIds = lists.stream().map(ListEntity::getEntityTypeId).distinct().toList();
      log.info("Getting entity type summary for entityTypeIds: {}", entityTypeIds);
      return entityTypeClient.getEntityTypeSummary(entityTypeIds)
        .stream()
        .collect(toMap(EntityTypeSummary::id, EntityTypeSummary::label));
    } catch (Exception exception) {
      log.error("Unexpected error when fetching summaries: {}", exception.getMessage(), exception);
      return Map.of();
    }
  }

  private UsersClient.User getCurrentUser() {
    log.info("Fetching user information for user id {}", executionContext.getUserId());
    try {
      return usersClient.getUser(executionContext.getUserId());
    } catch (Exception exception) {
      log.error("Unexpected error while fetching user info for id " + executionContext.getUserId(), exception);
      return new UsersClient.User(executionContext.getUserId(), Optional.empty());
    }
  }

  private void importListContentsFromAsyncQuery(ListEntity savedEntity, UsersClient.User currentUser, UUID queryId, TaskTimer timer) {
    savedEntity.refreshStarted(currentUser);
    // Save to ensure inProgressRefreshId is present
    savedEntity = listRepository.save(savedEntity);
    log.info("Attempting to refresh list with listId {}", savedEntity);
    listRefreshService.doAsyncSorting(
      savedEntity,
      queryId,
      registerShutdownTask(savedEntity, "Cancel refresh for list " + savedEntity.getId()),
      timer);
  }

  private String getUserFriendlyQuery(String fqlCriteria, EntityType entityType) {
    Fql fql = fqlService.getFql(fqlCriteria);
    return userFriendlyQueryService.getUserFriendlyQuery(fql.fqlCondition(), entityType);
  }

  private EntityType getEntityType(UUID entityTypeId) {
    return entityTypeClient.getEntityType(entityTypeId);
  }

  private AppShutdownService.ShutdownTask registerShutdownTask(ListEntity list, String taskName) {
    Runnable shutDownTask = () -> refreshFailedCallback.accept(list, new TaskTimer(), new RefreshInProgressDuringShutdownException(list));
    return appShutdownService.registerShutdownTask(executionContext, shutDownTask, taskName);
  }

  // The below method retrieves all fields from the entity type. We are using it as a
  // temporary workaround to supply fields in the list request. This maintains compatibility
  // until the UI has been updated to pass the fields parameter in a list request.
  private List<String> getFieldsFromEntityType(EntityType entityType) {
    List<String> fields = new ArrayList<>();
    entityType
      .getColumns()
      .forEach(col -> fields.add(col.getName()));
    return fields;
  }
}
