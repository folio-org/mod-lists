package org.folio.list.configuration;

import feign.Client;
import okhttp3.OkHttpClient;
import org.folio.list.rest.SystemUserClient;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserService;
import org.springframework.context.annotation.Bean;

public class SystemUserFeignConfig {

  @Bean
  public Client feignClient(FolioExecutionContext executionContext, SystemUserService systemUserService) {
    return new SystemUserClient(executionContext, systemUserService, new OkHttpClient());
  }
}

