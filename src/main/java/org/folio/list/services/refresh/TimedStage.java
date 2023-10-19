package org.folio.list.services.refresh;

public enum TimedStage {
  TOTAL,
  WRITE_START,
  REQUEST_QUERY,
  WAIT_FOR_QUERY_COMPLETION,
  IMPORT_RESULTS,
  WRITE_END,
}
