package org.folio.list.rest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.querytool.domain.dto.QueryIdentifier;
import org.folio.querytool.domain.dto.SubmitQuery;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "query")
public interface QueryClient {
  @PostExchange("")
  QueryIdentifier executeQuery(@RequestBody SubmitQuery submitQuery);

  @GetExchange("/{queryId}")
  QueryDetails getQuery(@PathVariable UUID queryId);

  @GetExchange("/{queryId}/sortedIds")
  List<List<String>> getSortedIds(
    @PathVariable UUID queryId,
    @RequestParam Integer offset,
    @RequestParam Integer limit
  );

  @PostExchange("/contents")
  List<Map<String, Object>> getContents(@RequestBody ContentsRequest contentsRequest);

  @PostExchange("/contents/privileged")
  List<Map<String, Object>> getContentsPrivileged(@RequestBody ContentsRequest contentsRequest);
}
