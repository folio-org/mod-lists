package org.folio.list.services;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.list.domain.ListEntity;
import org.folio.list.mapper.ListMigrationMapper;
import org.folio.list.repository.ListRepository;
import org.folio.list.repository.MigrationRepository;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.rest.EntityTypeClient.EntityTypeSummary;
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

  private final EntityTypeClient entityTypeClient;
  private final FolioExecutionContext executionContext;
  private final MigrationRepository migrationRepository;
  private final ListMigrationMapper mapper;
  private final ListRepository listRepository;
  private final MigrationClient migrationClient;
  private final SystemUserScopedExecutionService systemUserScopedExecutionService;

  private final AsyncTaskExecutor executor;

  /**
   * Upgrade a given list's query/entity type/field list.
   *
   * @param list
   * @return if the list was migrated or not (false indicates no changes needed, not an error state)
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
  public List<CompletableFuture<Boolean>> migrateAllLists() {
    String tenant = executionContext.getTenantId();

    return StreamSupport
      .stream(listRepository.findAll().spliterator(), true)
      .peek(list -> log.info("YYZ Processing list: {}", list.getId()))
      .filter(list -> list.getFqlQuery() != null)
      .filter(list -> !Boolean.TRUE.equals(list.getIsDeleted()))
      .map(list ->
        executor
          .submitCompletable(() ->
            // attempting to set the scope inside migrateList fails, as its in another thread,
            // and so we've lost all context. Therefore, we need to create the context here,
            // and specifically pass in the tenant, too.
            systemUserScopedExecutionService.executeSystemUserScoped(tenant, () -> migrateList(list))
          )
          .exceptionally(e -> {
            log.error("Error migrating list {}. This list may not function correctly", list, e);
            throw new CompletionException(e);
          })
      )
      .toList();
  }

  /**
   * Check that lists are up to date with the provided {@code latestVersion}
   */
  public void verifyListsAreUpToDate(String latestVersion) {
    String currentVersion = migrationRepository.getLatestMigratedVersion();
    if (currentVersion.equals(latestVersion)) {
      log.info("Lists are up to date!");
      return;
    }

    log.info("Lists are not up to date; migrating all lists from {} to {}", currentVersion, latestVersion);

    List<CompletableFuture<Boolean>> migrationFutures = migrateAllLists();
    try {
      CompletableFuture.allOf(migrationFutures.toArray(CompletableFuture[]::new)).join();
      log.info("All migrations completed successfully!");
    } catch (CompletionException e) {
      log.error("Error migrating lists", e);

      // if zero futures were joined, there will be no exceptions to catch,
      // so we can guarantee that here at least one failed
      if (migrationFutures.stream().allMatch(f -> f.isCompletedExceptionally())) {
        log.fatal("All migrations failed! Aborting migration...");
        throw e;
      } else {
        log.warn(
          "Some migrations failed, but not all. We will assume this is an issue with a few particular lists, not all..."
        );
      }
    }

    migrationRepository.setLatestMigratedVersion(latestVersion);
  }

  /**
   * Check that lists are up to date with the current FQM entity types version, fetched via API
   */
  public void verifyListsAreUpToDate() {
    String latestVersion = systemUserScopedExecutionService.executeSystemUserScoped(migrationClient::getVersion);
    verifyListsAreUpToDate(latestVersion);
  }

  /**
   * Marks all cross-tenant lists as private. This is a one-time migration, and is needed to catch
   * lists created before this requirement was enforced at the API level.
   *
   * @see https://folio-org.atlassian.net/browse/MODLISTS-152
   */
  public void handleModlists152CrossTenantSetToPrivateMigration() {
    if (Boolean.TRUE.equals(migrationRepository.hasModlists152CrossTenantSetToPrivateMigrationOccurred())) {
      log.info("MODLISTS-152 migration has already occurred, not doing extra work");
      return;
    }

    log.info("Migrating cross-tenant lists to private, to cover cases in MODLISTS-152");

    List<EntityTypeSummary> crossTenantEntityTypes = systemUserScopedExecutionService
      .executeSystemUserScoped(() -> entityTypeClient.getEntityTypeSummary(null))
      .entityTypes()
      .stream()
      .filter(EntityTypeSummary::crossTenantQueriesEnabled)
      .toList();

    log.info(
      "Determined that {} entity types are cross-tenant; setting their lists to private",
      () -> crossTenantEntityTypes.stream().map(EntityTypeSummary::label).toList()
    );

    List<UUID> crossTenantEntityTypeIds = crossTenantEntityTypes.stream().map(EntityTypeSummary::id).toList();

    List<ListEntity> updatedLists = StreamSupport
      .stream(listRepository.findAll().spliterator(), false)
      .filter(list -> crossTenantEntityTypeIds.contains(list.getEntityTypeId()))
      .map(list -> {
        list.setIsPrivate(true);
        return list;
      })
      .toList();

    listRepository.saveAll(updatedLists);

    migrationRepository.setModlists152CrossTenantSetToPrivateMigrationOccurred();

    log.info("Marked {} lists as private", updatedLists.size());
  }

  public void performTenantInstallMigrations() {
    this.verifyListsAreUpToDate();
    this.handleModlists152CrossTenantSetToPrivateMigration();
  }
}
