package org.folio.list.rest;

import org.folio.list.configuration.SystemUserFeignConfig;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "query/contents/privileged", configuration = FeignClientConfiguration.class)
public interface SystemUserQueryClient {

  @PostMapping("")
  List<Map<String, Object>> getContentsPrivileged(@RequestBody ContentsRequest contentsRequest);
}
