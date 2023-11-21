package org.folio.list.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EntityManagerFlushService {

  @PersistenceContext
  private final EntityManager entityManager;

  public void flush() {
    synchronized (entityManager) {
      entityManager.flush();
      entityManager.clear();
    }
  }
}
