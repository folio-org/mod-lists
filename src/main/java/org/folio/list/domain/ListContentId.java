package org.folio.list.domain;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
