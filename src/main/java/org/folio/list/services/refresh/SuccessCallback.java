package org.folio.list.services.refresh;

import org.folio.list.domain.ListEntity;
import org.folio.list.util.TaskTimer;

public interface SuccessCallback {
  void accept(ListEntity entity, int recordsCount, TaskTimer timer, boolean crossTenant);
}
