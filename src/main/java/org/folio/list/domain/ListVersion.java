package org.folio.list.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "list_versions")
public class ListVersion {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(updatable = false)
  private UUID id;

  @Column(name = "list_id")
  @NotNull
  private UUID listId;

  @Column(name = "name")
  @NotNull
  @Size(min = 1, max = 255)
  private String name;

  @Column(name = "description")
  @Size(max = 1024)
  private String description;

  @Column(name = "fql_query")
  private String fqlQuery;

  @Column(name = "fields")
  private List<String> fields;

  @Column(name = "updated_by")
  @NotNull
  private UUID updatedBy;

  @Column(name = "updated_by_username")
  @NotNull
  @Size(min = 1, max = 1024)
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

  @Column(name = "user_friendly_query")
  private String userFriendlyQuery;

  @Column(name = "cross_tenant")
  private boolean crossTenant;

  public void setDataFromListEntity(ListEntity listEntity) {
    listId = listEntity.getId();
    name = listEntity.getName();
    fqlQuery = listEntity.getFqlQuery();
    description = listEntity.getDescription();
    userFriendlyQuery = listEntity.getUserFriendlyQuery();
    fields = listEntity.getFields();
    updatedBy = listEntity.getUpdatedBy();
    updatedByUsername = listEntity.getUpdatedByUsername();
    updatedDate = listEntity.getUpdatedDate();
    version = listEntity.getVersion();
    isActive = listEntity.getIsActive();
    isPrivate = listEntity.getIsPrivate();
    crossTenant = listEntity.isCrossTenant();
  }
}
