package org.folio.list.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListContentId implements Serializable {

  @NotNull
  private UUID listId;


  @NotNull
  private UUID refreshId;

  @NotNull
  private UUID contentId;
}
