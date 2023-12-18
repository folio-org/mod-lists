package org.folio.list.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.folio.list.rest.UsersClient.User;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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

  @Column(name = "user_friendly_query")
  private String userFriendlyQuery;

  public void setDataFromListEntity(ListEntity listEntity, User user) {
    listId = listEntity.getId();
    name = listEntity.getName();
    fqlQuery = listEntity.getFqlQuery();
    description = listEntity.getDescription();
    userFriendlyQuery = listEntity.getUserFriendlyQuery();
    fields = listEntity.getFields();
    updatedBy = user.id();
    updatedByUsername = user.getFullName().orElse(user.id().toString());
    updatedDate = OffsetDateTime.now();
    version = listEntity.getVersion();
    isActive = listEntity.getIsActive();
    isPrivate = listEntity.getIsPrivate();
  }

}
