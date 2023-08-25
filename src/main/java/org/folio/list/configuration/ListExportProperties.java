package org.folio.list.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mod-lists.list-export")
public record ListExportProperties(@NotNull S3Properties s3Properties,
                                   @NotNull DataFetchProperties dataFetchProperties) {

  public int getBatchSize() {
    return dataFetchProperties.batchSize;
  }

  public record DataFetchProperties(@Min(10) int batchSize) {
  }

  public record S3Properties(@NotEmpty String bucket, @NotEmpty String region, @NotEmpty String endpoint,
                             boolean awsSdk, String accessKey, String secretKey) {
  }
}
