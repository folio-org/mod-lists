package org.folio.list.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "export_details")
public class ExportDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "export_id", updatable = false)
  @NotNull
  private UUID exportId;

  @NotNull
  @ManyToOne
  @JoinColumn(name = "list_id")
  private ListEntity list;

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
