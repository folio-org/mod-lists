package org.folio.list.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.list.repository.EntityId;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "export_details")
public class ExportDetails {

  @Id
  @EntityId
  @Column(name = "export_id", updatable = false)
  @NotNull
  private UUID exportId;

  @NotNull
  @ManyToOne
  @JoinColumn(name = "list_id")
  private ListEntity list;

  @NotNull
  @Column(name = "fields")
  private List<String> fields;

  @Column(name = "status")
  @NotNull
  @Enumerated(EnumType.STRING)
  private AsyncProcessStatus status;

  @Column(name = "created_by")
  @NotNull
  private UUID createdBy;

  @Column(name = "start_date")
  @NotNull
  private OffsetDateTime startDate;

  @Column(name = "end_date")
  private OffsetDateTime endDate;
}
