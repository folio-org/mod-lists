package org.folio.list.configuration;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.list.rest.EntityTypeClient;
import org.folio.list.rest.MigrationClient;
import org.folio.list.rest.QueryClient;
import org.folio.list.rest.UsersClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.NotFoundRestClientAdapterDecorator;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class HttpClientConfiguration {

  @Bean
  public EntityTypeClient entityTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(EntityTypeClient.class);
  }

  @Bean
  public MigrationClient migrationClient(
    HttpServiceProxyFactory factory
  ) {
    return factory.createClient(MigrationClient.class);
  }

  @Bean
  public QueryClient queryClient(HttpServiceProxyFactory factory) {
    return factory.createClient(QueryClient.class);
  }

  @Bean
  public UsersClient usersClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UsersClient.class);
  }
}
