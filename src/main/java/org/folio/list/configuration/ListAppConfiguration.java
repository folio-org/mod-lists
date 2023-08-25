package org.folio.list.configuration;

import lombok.RequiredArgsConstructor;
import org.folio.fql.FqlService;
import org.folio.fqm.lib.service.FqlValidationService;
import org.folio.fqm.lib.FQM;
import org.folio.list.domain.dto.ListConfiguration;
import org.folio.list.repository.ListContentsRepository;
import org.folio.list.repository.ListRefreshRepository;
import org.folio.list.services.refresh.DataBatchCallback;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.S3ClientFactory;
import org.folio.s3.client.S3ClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.function.Supplier;

@Configuration
@RequiredArgsConstructor
public class ListAppConfiguration {
  private final ListExportProperties listExportProperties;

  @Bean
  public FqlService fqlService() {
    return new FqlService();
  }

  @Bean
  public FqlValidationService fqlValidationService() {
    return FQM.fqlValidationService();
  }

  @Bean
  @Lazy // Do not connect to S3 when the application starts
  public FolioS3Client s3Client() {
    ListExportProperties.S3Properties s3Config = listExportProperties.s3Properties();
    S3ClientProperties s3Properties = S3ClientProperties.builder()
      .awsSdk(s3Config.awsSdk())
      .bucket(s3Config.bucket())
      .forcePathStyle(false)
      .region(s3Config.region())
      .endpoint(s3Config.endpoint())
      .accessKey(s3Config.accessKey())
      .secretKey(s3Config.secretKey())
      .build();
    return S3ClientFactory.getS3Client(s3Properties);
  }

  @Bean
  @ConfigurationProperties(prefix = "mod-lists.general")
  public ListConfiguration listConfiguration() {
    return new ListConfiguration();
  }

  @Bean
  public Supplier<DataBatchCallback> dataBatchCallbackSupplier(ListRefreshRepository listRefreshRepository,
                                                               ListContentsRepository listContentsRepository,
                                                               ListConfiguration listConfiguration) {
    return () -> new DataBatchCallback(listRefreshRepository, listContentsRepository, listConfiguration);
  }
}
