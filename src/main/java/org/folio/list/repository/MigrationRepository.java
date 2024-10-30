package org.folio.list.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// not really a Repository, but it felt like it belonged with its database friends.
// JPA doesn't like handling tables like this with no key/etc, so we just use native queries here.
@Log4j2
@Component
public class MigrationRepository {

  // EntityManager plays a little nicer with Spring's builtin transaction handling,
  // and overall has a nicer interface than the plain JdbcTemplate
  private final EntityManager entityManager;
  private final TransactionTemplate transactionTemplate;

  @Autowired
  public MigrationRepository(EntityManager entityManager, PlatformTransactionManager transactionManager) {
    this.entityManager = entityManager;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  private void initializeTable() {
    log.warn("No latest_migrated_version row found, storing initial values");
    entityManager
      .createNativeQuery(
        "INSERT INTO latest_migrated_version (version, modlists_152_cross_tenant_set_to_private) VALUES ('-1', false)"
      )
      .executeUpdate();
  }

  public String getLatestMigratedVersion() {
    return transactionTemplate.execute(status -> {
      try {
        return (String) entityManager
          .createNativeQuery("SELECT version FROM latest_migrated_version")
          .getSingleResult();
      } catch (NoResultException e) {
        initializeTable();
        return "-1";
      }
    });
  }

  public void setLatestMigratedVersion(String version) {
    if (getLatestMigratedVersion().equals(version)) {
      return;
    }

    log.info("Setting latest_migrated_version to {}", version);
    transactionTemplate.executeWithoutResult(status ->
      entityManager
        .createNativeQuery("UPDATE latest_migrated_version SET version = :version")
        .setParameter("version", version)
        .executeUpdate()
    );
  }

  /**
   * This flag indicates if the one-time migration to mark all cross-tenant lists as private has occurred.
   *
   * @see https://folio-org.atlassian.net/browse/MODLISTS-152
   */
  public Boolean hasModlists152CrossTenantSetToPrivateMigrationOccurred() {
    return transactionTemplate.execute(status -> {
      try {
        return (Boolean) entityManager
          .createNativeQuery("SELECT modlists_152_cross_tenant_set_to_private FROM latest_migrated_version")
          .getSingleResult();
      } catch (NoResultException e) {
        initializeTable();
        return false;
      }
    });
  }

  public void setModlists152CrossTenantSetToPrivateMigrationOccurred() {
    log.info("Setting modlists_152_cross_tenant_set_to_private migration flag to true");
    transactionTemplate.executeWithoutResult(status ->
      entityManager
        .createNativeQuery("UPDATE latest_migrated_version SET modlists_152_cross_tenant_set_to_private = true")
        .executeUpdate()
    );
  }
}
