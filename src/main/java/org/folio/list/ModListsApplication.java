package org.folio.list;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableFeignClients
@ConfigurationPropertiesScan
public class ModListsApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModListsApplication.class, args);
  }
}
