package org.folio.list.repository;

import java.io.Serializable;
import java.util.UUID;
import lombok.NoArgsConstructor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

@NoArgsConstructor
public class UUIDGenerator implements IdentifierGenerator {

  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object) {
    Serializable id =
      (Serializable) session.getEntityPersister(null, object).getIdentifier(object, session);
    return id != null ? id : UUID.randomUUID();
  }

  @Override
  public boolean allowAssignedIdentifiers() {
    return true;
  }
}
