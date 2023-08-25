package org.folio.list.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@IdClass(ListContentId.class)
@Table(name = "list_contents")
public class ListContent {
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
}
