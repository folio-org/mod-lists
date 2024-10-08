package org.folio.list.services;

import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

    log.info("Verifying lists are up to date");
    migrationService.verifyListsAreUpToDate();
  }
}
