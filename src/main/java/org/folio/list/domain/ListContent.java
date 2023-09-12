package org.folio.list.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
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
  private UUID contentId;

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
