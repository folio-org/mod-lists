package org.folio.list;

import org.folio.fql.config.FqlConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableFeignClients
@ConfigurationPropertiesScan
@ImportAutoConfiguration(classes = { FqlConfiguration.class })
public class ModListsApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModListsApplication.class, args);
  }
}
