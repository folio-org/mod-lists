package org.folio.list.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.folio.list.exception.InsufficientEntityTypePermissionsException;
import org.folio.list.services.CustomTenantService;
import org.folio.list.services.ListActions;
import org.folio.list.services.MigrationService;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CustomTenantServiceTest {

  @InjectMocks
  private CustomTenantService customTenantService;

  @Mock
  private PrepareSystemUserService prepareSystemUserService;

  @Mock
  private MigrationService migrationService;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(customTenantService, "systemUserRetryWaitMinutes", 10);
  }

  @Test
  void testTenantInstallTasks() {
    customTenantService.createOrUpdateTenant(new TenantAttributes());

    verify(prepareSystemUserService, times(1)).setupSystemUser();
    verify(migrationService, times(1)).performTenantInstallMigrations();
  }

  @ParameterizedTest
  @MethodSource("retryExceptionProvider")
  void testMigrationErrorRetry(Exception exception) {
    // Given a retryable migration error that fails for the first 5 seconds, then succeeds
    Instant finishTime = Instant.now().plus(5, ChronoUnit.SECONDS); // This needs to be greater than the max timeout on the retryTemplate in CustomTenantService
    AtomicInteger attempts = new AtomicInteger(0);
    doAnswer(invocation -> {
        attempts.incrementAndGet();
        if (Instant.now().isAfter(finishTime)) {
          return null;
        }
        throw exception;
      })
      .when(migrationService)
      .performTenantInstallMigrations();

    // When the tenant is installed
    customTenantService.createOrUpdateTenant(new TenantAttributes());

    // Then the migration should be retried until it succeeds
    // Verify that we didn't get any unexpected calls to the migration service and that it retried at least once
    verify(migrationService, times(attempts.get())).performTenantInstallMigrations();
    assertTrue(attempts.get() > 1, "Expected migration to be retried at least once");
  }

  static Stream<Exception> retryExceptionProvider() {
    return Stream.of(
      new InsufficientEntityTypePermissionsException(
        UUID.randomUUID(),
        ListActions.UPDATE,
        "User is missing permissions [{\"missing permission\"]}"
      ),
      // just exception
      FeignException.errorStatus("GET", Response.builder()
        .status(403)
        .reason("Forbidden")
        .request(Request.create(Request.HttpMethod.GET, "", Map.of(), null, null, null))
        .build()),
      // exception with 1 wrapper
      new CompletionException(FeignException.errorStatus("GET", Response.builder()
        .status(404)
        .reason("Not Found")
        .request(Request.create(Request.HttpMethod.GET, "", Map.of(), null, null, null))
        .build())),
      // exception with 2 wrappers
      new CompletionException(new CompletionException(FeignException.errorStatus("GET", Response.builder()
        .status(404)
        .reason("Not Found")
        .request(Request.create(Request.HttpMethod.GET, "", Map.of(), null, null, null))
        .build())))
    );
  }

  @Test
  void testMigrationWrappedErrorRetry() {
    // Given a retryable migration error that fails for the first 5 seconds, then succeeds
    Instant finishTime = Instant.now().plus(5, ChronoUnit.SECONDS); // This needs to be greater than the max timeout on the retryTemplate in CustomTenantService
    AtomicInteger attempts = new AtomicInteger(0);
    doAnswer(invocation -> {
        attempts.incrementAndGet();
        if (Instant.now().isAfter(finishTime)) {
          return null;
        }
        throw new RuntimeException(new InsufficientEntityTypePermissionsException(
          UUID.randomUUID(),
          ListActions.UPDATE,
          "User is missing permissions [{\"missing permission\"]}"
        ));
      })
      .when(migrationService)
      .performTenantInstallMigrations();

    // When the tenant is installed
    customTenantService.createOrUpdateTenant(new TenantAttributes());

    // Then the migration should be retried until it succeeds
    // Verify that we didn't get any unexpected calls to the migration service and that it retried at least once
    verify(migrationService, times(attempts.get())).performTenantInstallMigrations();
    assertTrue(attempts.get() > 1, "Expected migration to be retried at least once");
  }

  @Test
  void testMigrationErrorUnretryable() {
    // Given a un-retryable migration error that fails...
    AtomicInteger attempts = new AtomicInteger(0);
    doAnswer(invocation -> {
        attempts.incrementAndGet();
        throw new RuntimeException("kaboom");
      })
      .when(migrationService)
      .performTenantInstallMigrations();

    TenantAttributes attributes = new TenantAttributes();
    assertThrows(RuntimeException.class, () -> customTenantService.createOrUpdateTenant(attributes));

    verify(migrationService, times(1)).performTenantInstallMigrations();
    assertEquals(1, attempts.get());
  }
}
