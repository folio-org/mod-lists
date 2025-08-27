package org.folio.list.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.data.domain.Persistable;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@With
@IdClass(ListContentId.class)
@Table(name = "list_contents")
// Implements Persistable so that we can explicitly mark each object as new in the DB
public class ListContent implements Persistable<ListContentId> {

  public static final int SORT_SEQUENCE_START_NUMBER = 0;

  @Column(name = "list_id")
  @NotNull
  @Id
  private UUID listId;

  @Column(name = "refresh_id")
  @NotNull
  @Id
  private UUID refreshId;

  @Column(name = "content_id", updatable = false)
  @NotNull
  @Id
  private List<String> contentId;

  @Column(name = "sort_seq")
  private int sortSequence;

  @Override
  public ListContentId getId() {
    return new ListContentId(listId, refreshId, contentId);
  }

  @Override
  public boolean isNew() {
    // We treat ListContents data as immutable in the DB, so it's *always* a new row
    // Without this, on every insert, JPA does a SELECT to see if it's new before the INSERT
    return true;
  }
}
