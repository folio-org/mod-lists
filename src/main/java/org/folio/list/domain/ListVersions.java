package org.folio.list.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "list_versions")
public class ListVersions {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(updatable = false)
  private UUID id;

  @NotNull
  @ManyToOne
  @JoinColumn(name = "list_id")
  private ListEntity list;

  @Column(name = "name")
  @NotNull
  @Size(min = 4, max = 255)
  private String name;

  @Column(name = "description")
  @Size(min = 4, max = 255)
  private String description;

  @Column(name = "fql_query")
  @Size(min = 4, max = 1024)
  private String fqlQuery;

  @Column(name = "fields")
  private List<String> fields;

  @Column(name = "updated_by")
  @NotNull
  private UUID updatedBy;

  @Column(name = "updated_by_username")
  @NotNull
  @Size(min = 4, max = 1024)
  private String updatedByUsername;

  @Column(name = "updated_date")
  private OffsetDateTime updatedDate;

  @Column(name = "is_active")
  @NotNull
  private Boolean isActive;

  @Column(name = "is_private")
  @NotNull
  private Boolean isPrivate;


  @Column(name = "version")
  @NotNull
  private int version;

}
