package org.folio.list.configuration;

import org.folio.list.rest.EntityTypeClient;
import org.folio.list.rest.MigrationClient;
import org.folio.list.rest.QueryClient;
import org.folio.list.rest.UsersClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfiguration {

  @Bean
  public EntityTypeClient entityTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(EntityTypeClient.class);
  }

  @Bean
  public MigrationClient migrationClient(HttpServiceProxyFactory factory) {
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
