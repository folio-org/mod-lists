package org.folio.list.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.folio.list.domain.ListEntity;
import org.folio.list.mapper.ListMigrationMapper;
import org.folio.list.repository.ListRepository;
import org.folio.list.repository.MigrationRepository;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.rest.MigrationClient;
import org.folio.list.services.MigrationService;
import org.folio.list.util.TestDataFixture;
import org.folio.querytool.domain.dto.FqmMigrateResponse;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

@ExtendWith(MockitoExtension.class)
class MigrationServiceTest {

  static final FqmMigrateResponse CHANGED_RESPONSE = new FqmMigrateResponse()
    .entityTypeId(UUID.fromString("21b97d9f-46e8-5cde-ab64-d09da7403780"))
    .fqlQuery("new query")
    .fields(List.of("a", "b", "c"));

  @Mock
  EntityTypeClient entityTypeClient;

  @Mock
  FolioExecutionContext executionContext;

  @Mock
  MigrationRepository migrationRepository;

  @Mock
  ListRepository listRepository;

  @Mock
  MigrationClient migrationClient;

  @Spy
  ListMigrationMapper mapper;

  @Mock
  SystemUserScopedExecutionService systemUserScopedExecutionService;

  @Mock
  AsyncTaskExecutor executor;

  @InjectMocks
  MigrationService migrationService;

  @Test
  void testMigrateListWithNoQuery() {
    ListEntity list = TestDataFixture.getListEntityWithoutQuery();

    assertThrows(IllegalArgumentException.class, () -> migrationService.migrateList(list));
  }

  @Test
  void testMigrateListWithNoChange() {
    ListEntity sourceList = TestDataFixture.getListEntityWithSuccessRefresh();

    when(migrationClient.migrate(mapper.toMigrationRequest(sourceList)))
      .thenReturn(new FqmMigrateResponse().fqlQuery(sourceList.getFqlQuery()));

    assertThat(migrationService.migrateList(sourceList), is(false));

    verify(migrationClient, times(1)).migrate(any());
    verifyNoMoreInteractions(migrationClient);
    verifyNoInteractions(migrationRepository, listRepository);
  }

  @Test
  void testMigrateListWithChanges() {
    ListEntity sourceList = TestDataFixture.getListEntityWithSuccessRefresh();

    when(migrationClient.migrate(mapper.toMigrationRequest(sourceList))).thenReturn(CHANGED_RESPONSE);

    assertThat(migrationService.migrateList(sourceList), is(true));

    verify(migrationClient, times(1)).migrate(any());
    verify(listRepository, times(1)).save(mapper.updateListWithMigration(sourceList, CHANGED_RESPONSE));
    verifyNoMoreInteractions(migrationClient, listRepository);
    verifyNoInteractions(migrationRepository);
  }

  @Test
  void testMigrateAllLists() {
    List<ListEntity> sourceLists = List.of(
      TestDataFixture.getListEntityWithSuccessRefresh(UUID.fromString("e99fa190-7d95-54f5-bba7-6ca661dcc01d")),
      TestDataFixture.getListEntityWithSuccessRefresh(UUID.fromString("de13facd-ac98-575b-be95-619fbf387c8f")),
      TestDataFixture.getListEntityWithoutQuery(),
      TestDataFixture.getListEntityWithSuccessRefresh().withIsDeleted(true),
      TestDataFixture.getListEntityWithSuccessRefresh(UUID.fromString("f778600e-d680-52ff-90c7-3e524e555d29"))
    );

    when(executionContext.getTenantId()).thenReturn("tenant");
    when(systemUserScopedExecutionService.executeSystemUserScoped(eq("tenant"), any()))
      .thenAnswer(invocation -> ((Callable<?>) invocation.getArgument(1)).call());
    when(listRepository.findAll()).thenReturn(sourceLists);
    when(migrationClient.migrate(any()))
      .thenReturn(
        CHANGED_RESPONSE,
        new FqmMigrateResponse().fqlQuery(sourceLists.get(1).getFqlQuery()),
        CHANGED_RESPONSE
      );

    // the world's best async implementation™
    when(executor.submitCompletable((Callable<?>) any(Callable.class)))
      .thenAnswer(invocation -> {
        Callable<?> task = invocation.getArgument(0);
        task.call();
        return CompletableFuture.completedFuture(null);
      });

    migrationService.migrateAllLists();

    verify(migrationClient, times(3)).migrate(any());
    verify(listRepository, times(1)).findAll();
    verify(listRepository, times(2)).save(any());
    verifyNoMoreInteractions(migrationClient, listRepository);
    verifyNoInteractions(migrationRepository);
  }

  @Test
  void testMigrationContinuesWhenSomeFail() {
    List<ListEntity> sourceLists = List.of(
      TestDataFixture.getListEntityWithSuccessRefresh(UUID.fromString("aaaaaaaa-7d95-54f5-bba7-6ca661dcc01d")),
      TestDataFixture.getListEntityWithSuccessRefresh(UUID.fromString("ffffffff-ac98-575b-be95-619fbf387c8f"))
    );

    when(migrationRepository.getLatestMigratedVersion()).thenReturn("old");
    when(executionContext.getTenantId()).thenReturn("tenant");
    when(listRepository.findAll()).thenReturn(sourceLists);

    // the world's best async implementation™
    when(executor.submitCompletable((Callable<?>) any(Callable.class)))
      .thenReturn(CompletableFuture.completedFuture(null))
      .thenReturn(CompletableFuture.failedFuture(new RuntimeException("oh no!")));

    assertDoesNotThrow(() -> migrationService.verifyListsAreUpToDate("new"));

    verify(migrationRepository, times(1)).getLatestMigratedVersion();
    verify(migrationRepository, times(1)).setLatestMigratedVersion("new");
  }

  @Test
  void testMigrationFailsWhenAllFail() {
    List<ListEntity> sourceLists = List.of(
      TestDataFixture.getListEntityWithSuccessRefresh(UUID.fromString("aaaaaaaa-7d95-54f5-bba7-6ca661dcc01d")),
      TestDataFixture.getListEntityWithSuccessRefresh(UUID.fromString("ffffffff-ac98-575b-be95-619fbf387c8f"))
    );

    when(migrationRepository.getLatestMigratedVersion()).thenReturn("old");
    when(executionContext.getTenantId()).thenReturn("tenant");
    when(listRepository.findAll()).thenReturn(sourceLists);

    // the world's best async implementation™
    when(executor.submitCompletable((Callable<?>) any(Callable.class)))
      .thenReturn(CompletableFuture.failedFuture(new RuntimeException("oh no!")));

    assertThrows(CompletionException.class, () -> migrationService.verifyListsAreUpToDate("new"));

    verify(migrationRepository, times(1)).getLatestMigratedVersion();
    verifyNoMoreInteractions(migrationRepository);
  }

  @Test
  void testVerifyListsUpToDateWhenUpToDate() {
    when(migrationRepository.getLatestMigratedVersion()).thenReturn("current");

    migrationService.verifyListsAreUpToDate("current");

    verify(migrationRepository, times(1)).getLatestMigratedVersion();
    verifyNoMoreInteractions(migrationRepository);
    verifyNoInteractions(listRepository, migrationClient);
  }

  @Test
  void testVerifyListsUpToDateWhenUpdateNeeded() {
    when(migrationRepository.getLatestMigratedVersion()).thenReturn("old");
    when(listRepository.findAll()).thenReturn(List.of());

    migrationService.verifyListsAreUpToDate("new");

    verify(migrationRepository, times(1)).getLatestMigratedVersion();
    verify(migrationRepository, times(1)).setLatestMigratedVersion("new");
    verify(listRepository, times(1)).findAll();
    verifyNoMoreInteractions(migrationRepository, listRepository);
    verifyNoInteractions(migrationClient);
  }

  @Test
  void testVerifyListsUpToDateWithNoArgAndUpdateNeeded() {
    when(migrationClient.getVersion()).thenReturn("new");
    when(migrationRepository.getLatestMigratedVersion()).thenReturn("old");
    when(listRepository.findAll()).thenReturn(List.of());
    when(systemUserScopedExecutionService.executeSystemUserScoped(any()))
      .thenAnswer(invocation -> ((Callable<?>) invocation.getArgument(0)).call());

    migrationService.verifyListsAreUpToDate();

    verify(migrationClient, times(1)).getVersion();
    verify(migrationRepository, times(1)).getLatestMigratedVersion();
    verify(migrationRepository, times(1)).setLatestMigratedVersion("new");
    verify(listRepository, times(1)).findAll();
    verifyNoMoreInteractions(migrationClient, migrationRepository, listRepository);
  }

  @Test
  void testModlists152CrossTenantSetToPrivateMigration() {
    UUID crossTenantEntityType = UUID.fromString("cf238d2f-6b39-5fcb-a771-cb7610b4ede2");
    UUID nonCrossTenantEntityType = UUID.fromString("f16bc5c3-e40a-59db-b954-f753e28628e2");

    ListEntity listToUpdate = spy(new ListEntity().withEntityTypeId(crossTenantEntityType));
    ListEntity listToNotUpdate = spy(new ListEntity().withEntityTypeId(nonCrossTenantEntityType));

    when(migrationRepository.hasModlists152CrossTenantSetToPrivateMigrationOccurred()).thenReturn(false);
    when(entityTypeClient.getEntityTypeSummary(null))
      .thenReturn(
        new EntityTypeClient.EntityTypeSummaryResponse(
          List.of(
            new EntityTypeClient.EntityTypeSummary(crossTenantEntityType, "cross-tenant et", true),
            new EntityTypeClient.EntityTypeSummary(nonCrossTenantEntityType, "not cross tenant et", false)
          ),
          null
        )
      );
    when(listRepository.findAll()).thenReturn(List.of(listToUpdate, listToNotUpdate));
    when(systemUserScopedExecutionService.executeSystemUserScoped(any()))
      .thenAnswer(invocation -> ((Callable<?>) invocation.getArgument(0)).call());

    migrationService.handleModlists152CrossTenantSetToPrivateMigration();

    ArgumentCaptor<List<ListEntity>> listCaptor = ArgumentCaptor.forClass(List.class);
    verify(listRepository, times(1)).saveAll(listCaptor.capture());
    assertThat(listCaptor.getValue(), contains(listToUpdate));

    // .filter calls getEntityTypeId
    verify(listToNotUpdate, times(1)).getEntityTypeId();
    verify(listToUpdate, times(1)).getEntityTypeId();
    verify(listToUpdate, times(1)).setIsPrivate(true);
    verifyNoMoreInteractions(listToUpdate, listToNotUpdate);

    verify(migrationRepository, times(1)).hasModlists152CrossTenantSetToPrivateMigrationOccurred();
    verify(migrationRepository, times(1)).setModlists152CrossTenantSetToPrivateMigrationOccurred();
    verifyNoMoreInteractions(migrationRepository, listRepository);
  }

  @Test
  void testTenantInstallNoUpdatesNeeded() {
    when(migrationClient.getVersion()).thenReturn("current");
    when(migrationRepository.getLatestMigratedVersion()).thenReturn("current");
    when(migrationRepository.hasModlists152CrossTenantSetToPrivateMigrationOccurred()).thenReturn(true);
    when(systemUserScopedExecutionService.executeSystemUserScoped(any()))
      .thenAnswer(invocation -> ((Callable<?>) invocation.getArgument(0)).call());

    migrationService.performTenantInstallMigrations();

    verify(migrationClient, times(1)).getVersion();
    verify(migrationRepository, times(1)).getLatestMigratedVersion();
    verify(migrationRepository, times(1)).hasModlists152CrossTenantSetToPrivateMigrationOccurred();
    verifyNoMoreInteractions(migrationClient, migrationRepository, listRepository);
  }
}
