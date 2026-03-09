package org.folio.list.configuration;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@Log4j2
public class DataSourceConfiguration {

  @Bean
  @ConfigurationProperties("spring.datasource.writer")
  public DataSourceProperties writerDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Primary
  @Qualifier("dataSource")
  @Bean(name = "dataSource")
  @ConfigurationProperties("spring.datasource.hikari")
  public DataSource writerDataSource() {
    return writerDataSourceProperties()
      .initializeDataSourceBuilder()
      .build();
  }

  @Primary
  @Qualifier("jdbcTemplate")
  @Bean(name = "jdbcTemplate")
  public JdbcTemplate writerJdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

}
