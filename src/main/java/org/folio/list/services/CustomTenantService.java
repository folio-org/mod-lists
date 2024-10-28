package org.folio.list.services;

import lombok.extern.log4j.Log4j2;
import org.folio.list.exception.InsufficientEntityTypePermissionsException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Log4j2
@Primary
@Service
public class CustomTenantService extends TenantService {

  protected final MigrationService migrationService;
  protected final PrepareSystemUserService prepareSystemUserService;

  @Autowired
  public CustomTenantService(
    JdbcTemplate jdbcTemplate,
    FolioExecutionContext context,
    FolioSpringLiquibase folioSpringLiquibase,
    MigrationService migrationService,
    PrepareSystemUserService prepareSystemUserService
  ) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.migrationService = migrationService;
    this.prepareSystemUserService = prepareSystemUserService;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    log.info("Initializing system user");
    prepareSystemUserService.setupSystemUser();

    // In Eureka, the system user often takes a short bit of time for its permissions to be assigned, so retry in the
    // case of failures related to missing permissions
    RetryTemplate.builder()
      .retryOn(InsufficientEntityTypePermissionsException.class)
      .exponentialBackoff(Duration.of(2, ChronoUnit.SECONDS), 1.5, Duration.of(1, ChronoUnit.MINUTES))
      .withTimeout(Duration.of(2, ChronoUnit.MINUTES))
      .build()
      .execute(ctx -> {
        log.info("Verifying lists are up to date. Attempt #" + (ctx.getRetryCount() + 1));
        migrationService.verifyListsAreUpToDate();
        return null;
      }, ctx -> {
        log.error("Unable to verify lists are up to date", ctx.getLastThrowable());
        // This RetryTemplate only deals with InsufficientEntityTypePermissionsException, so we know that's what this
        // Throwable really is. Rethrow it to fail the tenant update process.
        throw (InsufficientEntityTypePermissionsException) ctx.getLastThrowable();
      });
  }
}
