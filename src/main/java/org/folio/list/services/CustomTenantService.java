package org.folio.list.services;

import feign.FeignException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.folio.list.exception.InsufficientEntityTypePermissionsException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Log4j2
@Primary
@Service
public class CustomTenantService extends TenantService {
  @Value("${mod-lists.general.system-user-retry-wait-minutes:10}")
  private int systemUserRetryWaitMinutes;

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
    // case of failures related to missing permissions or general Feign exceptions
    // for cases in which mod-fqm tries to invoke mod-roles-keycloak and cannot retrieve roles for a system user that has not yet been created.
    RetryTemplate.builder()
      .retryOn(List.of(InsufficientEntityTypePermissionsException.class, FeignException.class))
      .exponentialBackoff(Duration.of(2, ChronoUnit.SECONDS), 1.5, Duration.of(1, ChronoUnit.MINUTES))
      .withTimeout(Duration.of(systemUserRetryWaitMinutes, ChronoUnit.MINUTES))
      .build()
      .execute(ctx -> {
        int attempt = (ctx.getRetryCount() + 1);
        log.info("Performing tenant install migrations. Attempt #" + attempt);
        try {
          migrationService.performTenantInstallMigrations();
        } catch (Exception e) {
          // Deal with wrapped permission exceptions by unwrapping and rethrowing the original exception.
          log.error("Exception during tenant install migration (attempt # {} )", attempt, e);

          checkExceptionEligibleForRetry(e); // Retry in case when this method throws an exception

          throw e; // Don't retry
        }
        return null;
      }, ctx -> {
        log.error("Unable to perform tenant install migration activities", ctx.getLastThrowable());
        // Rethrow it to fail the tenant update process.
        throw new RuntimeException(ctx.getLastThrowable());
      });
  }

  private void checkExceptionEligibleForRetry(Throwable e) {
    List<Throwable> causes = ExceptionUtils.getThrowableList(e);
    for (Throwable t : causes) {
      if (t instanceof InsufficientEntityTypePermissionsException ietpe) {
        log.info("Make retry of InsufficientEntityTypePermissionsException with message: {}", ietpe.getMessage());
        throw ietpe;
      }
      if (t instanceof FeignException fe) {
        log.info("Make retry of FeignException with message: {}", fe.getMessage());
        throw  fe;
      }
    }
  }
}
