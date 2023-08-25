package org.folio.list.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Contains the actions that can be performed on a list
 */
@RequiredArgsConstructor
@Getter
public enum ListActions {
  CREATE("create"),
  READ("read"),
  UPDATE("update"),
  DELETE("delete"),
  REFRESH("refresh"),
  EXPORT("export"),
  CANCEL_REFRESH("cancel_refresh"),
  CANCEL_EXPORT("cancel_export");

  private final String name;
}
