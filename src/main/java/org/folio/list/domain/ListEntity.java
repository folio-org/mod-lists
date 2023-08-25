package org.folio.list.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.list.domain.dto.ListUpdateRequestDTO;
import org.folio.list.exception.AbstractListException;
import org.folio.list.rest.UsersClient.User;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "list_details")
public class ListEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(updatable = false)
  private UUID id;

  @Column(name = "name")
  @NotNull
  @Size(min = 4, max = 255)
  private String name;

  @Column(name = "description")
  @Size(min = 4, max = 255)
  private String description;

  @Column(name = "entity_type_id")
  @NotNull
  private UUID entityTypeId;

  @Column(name = "fql_query")
  @Size(min = 4, max = 1024)
  private String fqlQuery;

  @Column(name = "fields")
  private List<String> fields;

  @Column(name = "created_by")
  @NotNull
  private UUID createdBy;

  @Column(name = "created_by_username")
  @NotNull
  @Size(min = 4, max = 64)
  private String createdByUsername;

  @Column(name = "created_date")
  private OffsetDateTime createdDate;

  @Column(name = "is_active")
  @NotNull
  private Boolean isActive;

  @Column(name = "is_private")
  @NotNull
  private Boolean isPrivate;

  @Column(name = "is_canned")
  @NotNull
  private Boolean isCanned;

  @Column(name = "updated_by")
  private UUID updatedBy;

  @Column(name = "updated_by_username")
  private String updatedByUsername;

  @Column(name = "updated_date")
  private OffsetDateTime updatedDate;

  @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "in_progress_refresh_id")
  private ListRefreshDetails inProgressRefresh;

  @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "success_refresh_id")
  private ListRefreshDetails successRefresh;

  @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "failed_refresh_id")
  private ListRefreshDetails failedRefresh;

  @Column(name = "version")
  @NotNull
  private int version;

  @Column(name = "user_friendly_query")
  private String userFriendlyQuery;

  public boolean isRefreshing() {
    return inProgressRefresh != null;
  }

  public boolean isRefreshed() {
    return successRefresh != null;
  }

  public boolean refreshFailed() {
    return failedRefresh != null;
  }

  public int getContentVersion() {
    return successRefresh == null ? 0 : successRefresh.getContentVersion();
  }

  public int getRecordsCount() {
    return isRefreshed() ? successRefresh.getRecordsCount() : 0;
  }

  public Optional<UUID> getInProgressRefreshId() {
    return isRefreshing() ? Optional.of(getInProgressRefresh().getId()) : Optional.empty();
  }

  public void refreshStarted(User startedBy) {
    this.inProgressRefresh = ListRefreshDetails.builder()
      .id(UUID.randomUUID())
      .status(AsyncProcessStatus.IN_PROGRESS)
      .listId(id)
      .refreshedBy(startedBy.id())
      .refreshedByUsername(startedBy.getFullName().orElse(startedBy.id().toString()))
      .refreshStartDate(OffsetDateTime.now())
      .build();
  }

  public void refreshCompleted(int recordsCount) {
    ListRefreshDetails refresh = this.inProgressRefresh;
    refresh.setStatus(AsyncProcessStatus.SUCCESS);
    refresh.setRefreshEndDate(OffsetDateTime.now());
    refresh.setRecordsCount(recordsCount);
    refresh.setContentVersion(getContentVersion() + 1);
    successRefresh = refresh;
    failedRefresh = null;
    inProgressRefresh = null;
  }

  public void refreshFailed(Throwable failureReason) {
    ListRefreshDetails refresh = this.inProgressRefresh;
    String errorCode = failureReason instanceof AbstractListException exception ?
      exception.getError().getCode() : "unexpected.error";
    refresh.setErrorCode(errorCode);
    refresh.setErrorMessage(failureReason.getMessage());
    refresh.setStatus(AsyncProcessStatus.FAILED);
    refresh.setRefreshEndDate(OffsetDateTime.now());
    failedRefresh = refresh;
    inProgressRefresh = null;
  }

  public void refreshCancelled(UUID cancelledBy) {
    inProgressRefresh.setStatus(AsyncProcessStatus.CANCELLED);
    inProgressRefresh.setCancelledBy(cancelledBy);
    setInProgressRefresh(null);
  }

  public void update(ListUpdateRequestDTO request,
                     User newUpdatedBy, String newUserFriendlyQuery) {
    name = request.getName();
    description = request.getDescription();
    fqlQuery = request.getFqlQuery();
    userFriendlyQuery = newUserFriendlyQuery;
    fields = request.getFields();
    isActive = request.getIsActive();
    isPrivate = request.getIsPrivate();
    updatedBy = newUpdatedBy.id();
    updatedByUsername = newUpdatedBy.getFullName().orElse(newUpdatedBy.id().toString());
    updatedDate = OffsetDateTime.now();
    version = version + 1;
  }
}
