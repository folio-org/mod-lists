package org.folio.list.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.folio.list.services.CustomTenantService;
import org.folio.list.services.MigrationService;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomTenantServiceTest {

  @InjectMocks
  private CustomTenantService customTenantService;

  @Mock
  private PrepareSystemUserService prepareSystemUserService;

  @Mock
  private MigrationService migrationService;

  @Test
  void testTenantInstallTasks() {
    customTenantService.createOrUpdateTenant(new TenantAttributes());

    verify(prepareSystemUserService, times(1)).setupSystemUser();
    verify(migrationService, times(1)).verifyListsAreUpToDate();
  }
}
