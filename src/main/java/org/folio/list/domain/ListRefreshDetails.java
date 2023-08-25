package org.folio.list.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Builder()
@Table(name = "list_refresh_details")
@AllArgsConstructor
@NoArgsConstructor
public class ListRefreshDetails {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(updatable = false)
  private UUID id;
  @Column(name = "list_id")
  @NotNull
  private UUID listId;
  @Column(name = "status")
  @NotNull
  @Enumerated(EnumType.STRING)
  private AsyncProcessStatus status;
  @Column(name = "refresh_start_date")
  @NotNull
  private OffsetDateTime refreshStartDate;
  @Column(name = "refresh_end_date")
  private OffsetDateTime refreshEndDate;
  @Column(name = "refreshed_by")
  @NotNull
  private UUID refreshedBy;
  @Column(name = "refreshed_by_username")
  @NotNull
  @Size(min = 4, max = 64)
  private String refreshedByUsername;
  @Column
  private UUID cancelledBy;
  @Column(name = "records_count")
  private Integer recordsCount;
  @Column(name = "content_version")
  private Integer contentVersion;
  @Column(name = "error_code")
  @Size(max = 64)
  private String errorCode;
  @Column(name = "error_message")
  @Size(max = 1024)
  private String errorMessage;
}
