package org.folio.list;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAsync
@EnableResilientMethods
@EnableTransactionManagement
@ConfigurationPropertiesScan
@SpringBootApplication
public class ModListsApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModListsApplication.class, args);
  }
}
