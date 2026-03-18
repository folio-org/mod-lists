package org.folio.list.configuration;

import feign.Client;
import okhttp3.OkHttpClient;
import org.folio.list.rest.SystemUserClient;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.EnrichUrlAndHeadersClient;
import org.folio.spring.service.SystemUserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

public class SystemUserFeignConfig {

  @Bean
  @ConditionalOnBean(SystemUserService.class)
  public Client systemUserFeignClient(FolioExecutionContext executionContext, SystemUserService systemUserService) {
    return new SystemUserClient(executionContext, systemUserService, new OkHttpClient());
  }

  @Bean
  @ConditionalOnMissingBean(SystemUserService.class)
  public Client defaultFeignClient(FolioExecutionContext executionContext) {
    return new EnrichUrlAndHeadersClient(executionContext, new OkHttpClient());
  }
}

