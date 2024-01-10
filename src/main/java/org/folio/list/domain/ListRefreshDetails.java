package org.folio.list.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Builder
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

  @Column(name = "list_version")
  private Integer listVersion;

  @Column(name = "metadata")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, String> metadata;
}
