package org.folio.list.services;

import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.list.domain.ListEntity;
import org.folio.list.mapper.ListMigrationMapper;
import org.folio.list.repository.LatestMigratedVersionRepository;
import org.folio.list.repository.ListRepository;
import org.folio.list.rest.MigrationClient;
import org.folio.querytool.domain.dto.FqmMigrateResponse;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class MigrationService {

  private final FolioExecutionContext executionContext;
  private final LatestMigratedVersionRepository latestMigratedVersionRepository;
  private final ListMigrationMapper mapper;
  private final ListRepository listRepository;
  private final MigrationClient migrationClient;
  private final SystemUserScopedExecutionService systemUserScopedExecutionService;

  private final AsyncTaskExecutor executor;

  /**
   * Upgrade a given list's query/entity type/field list.
   *
   * @param list
   * @return if the list was migrated or not (false indicates no changes needed, not an error)
   * @throws IllegalArgumentException if the list does not contain a query
   */
  public boolean migrateList(ListEntity list) {
    if (list.getFqlQuery() == null) {
      throw new IllegalArgumentException("FQL query is required for migration");
    }

    FqmMigrateResponse result = migrationClient.migrate(mapper.toMigrationRequest(list));

    // the query contains the version, so even if there were no substantial changes,
    // a migration will still change update that
    if (result.getFqlQuery().equals(list.getFqlQuery())) {
      log.info("Attempted migration of list {} yielded no changes", list.getId());
      return false;
    }

    listRepository.save(mapper.updateListWithMigration(list, result));

    log.info("Upgraded list {}", list.getId());

    return true;
  }

  /**
   * Migrate ALL lists in the database, asynchronously.
   *
   * <strong>This does NOT check if this is actually necessary, and should only be invoked when the
   * caller is certain that lists need to be migrated.</strong>
   */
  public CompletableFuture<Void> migrateAllLists() {
    String tenant = executionContext.getTenantId();

    return CompletableFuture.allOf(
      StreamSupport
        .stream(listRepository.findAll().spliterator(), true)
        .filter(list -> list.getFqlQuery() != null)
        .filter(list -> !Boolean.TRUE.equals(list.getIsDeleted()))
        .map(list ->
          executor.submitCompletable(() ->
            // attempting to set the scope inside migrateList fails, as its in another thread,
            // and so we've lost all context. Therefore, we need to create the context here,
            // and specifically pass in the tenant, too.
            systemUserScopedExecutionService.executeSystemUserScoped(
              tenant,
              () -> {
                migrateList(list);
                return null;
              }
            )
          )
        )
        .toArray(s -> new CompletableFuture[s])
    );
  }

  /**
   * Check that lists are up to date with the provided {@code latestVersion}
   */
  public void verifyListsAreUpToDate(String latestVersion) {
    String currentVersion = latestMigratedVersionRepository.getLatestMigratedVersion();
    if (currentVersion.equals(latestVersion)) {
      log.info("Lists are up to date!");
      return;
    }

    log.info("Lists are not up to date; migrating all lists from {} to {}", currentVersion, latestVersion);
    migrateAllLists().join();
    latestMigratedVersionRepository.setLatestMigratedVersion(latestVersion);
  }

  /**
   * Check that lists are up to date with the current FQM entity types version, fetched via API
   */
  public void verifyListsAreUpToDate() {
    String latestVersion = systemUserScopedExecutionService.executeSystemUserScoped(migrationClient::getVersion);
    verifyListsAreUpToDate(latestVersion);
  }
}
