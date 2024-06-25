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

  protected final PrepareSystemUserService prepareSystemUserService;

  protected final QueryMigrationService queryMigrationService;


  @Autowired
  public CustomTenantService(
    JdbcTemplate jdbcTemplate,
    FolioExecutionContext context,
    FolioSpringLiquibase folioSpringLiquibase,
    PrepareSystemUserService prepareSystemUserService,
    QueryMigrationService queryMigrationService
  ) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.prepareSystemUserService = prepareSystemUserService;
    this.queryMigrationService = queryMigrationService;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    log.info("Initializing system user");
   prepareSystemUserService.setupSystemUser();
    log.info("Initializing migrating queries");
   queryMigrationService.migratingQueries();
  }
}
