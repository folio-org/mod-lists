package org.folio.list.rest;

import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.querytool.domain.dto.QueryIdentifier;
import org.folio.querytool.domain.dto.SubmitQuery;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "query")
public interface QueryClient {

  @PostMapping("")
  QueryIdentifier executeQuery(@RequestBody SubmitQuery submitQuery);

  @GetMapping("/{queryId}")
  QueryDetails getQuery(@RequestHeader UUID queryId);

  @GetMapping("/{queryId}")
  QueryDetails getQuery(@RequestHeader UUID queryId, @RequestParam boolean includeResults, @RequestParam Integer offset, @RequestParam Integer limit);

  @PostMapping("/contents")
  List<Map<String, Object>> getContents(@RequestBody ContentsRequest contentsRequest);

  @PostMapping("/contents/privileged")
  List<Map<String, Object>> getContentsPrivileged(@RequestBody ContentsRequest contentsRequest);
}
