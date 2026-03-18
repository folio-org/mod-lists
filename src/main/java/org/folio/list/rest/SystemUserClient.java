package org.folio.list.rest;

import feign.Client;
import feign.Request;
import feign.Response;
import feign.okhttp.OkHttpClient;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.model.SystemUser;
import org.folio.spring.service.SystemUserService;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class SystemUserClient implements Client {

  private final FolioExecutionContext executionContext;
  private final SystemUserService systemUserService;
  private final OkHttpClient delegate;

  public SystemUserClient(FolioExecutionContext executionContext, SystemUserService systemUserService, okhttp3.OkHttpClient okHttpClient) {
    this.executionContext = executionContext;
    this.systemUserService = systemUserService;
    this.delegate = new OkHttpClient(okHttpClient);
  }

  public Response execute(Request request, Request.Options options) throws IOException {
    String url = prepareUrl(request.url(), this.executionContext);
    Map<String, Collection<String>> allHeaders = prepareHeaders(request, this.executionContext);
    Request requestWithUrl = Request.create(request.httpMethod(), url, allHeaders, request.body(), request.charset(), request.requestTemplate());
    log.debug("FolioExecutionContext: {};\nPrepared the Feign Client Request: {} with headers {};\nCurrent thread: {}", this.executionContext, requestWithUrl, allHeaders, Thread.currentThread().getName());
    return this.delegate.execute(requestWithUrl, options);
  }

  static String prepareUrl(String requestUrl, FolioExecutionContext context) {
    String okapiUrl = context.getOkapiUrl();
    if (okapiUrl == null) {
      return requestUrl;
    } else {
      okapiUrl = StringUtils.appendIfMissing(okapiUrl, "/");
      return requestUrl.replace("http://", okapiUrl);
    }
  }

  Map<String, Collection<String>> prepareHeaders(Request request, FolioExecutionContext context) {
    Map<String, Collection<String>> allHeaders = new HashMap<>(request.headers());
    Map<String, Collection<String>> okapiHeaders = new HashMap<>(context.getOkapiHeaders());
    String okapiTenant = okapiHeaders
      .getOrDefault("x-okapi-tenant", List.of())
      .stream()
      .findFirst()
      .orElse(null);
    if (okapiTenant != null) {
      SystemUser authedSystemUser = systemUserService.getAuthedSystemUser(okapiTenant);
      okapiHeaders.put("x-okapi-token", List.of(authedSystemUser.token().accessToken()));
    }
    allHeaders.putAll(okapiHeaders);
    context.getAllHeaders()
      .keySet()
      .stream()
      .filter("Accept-Language"::equalsIgnoreCase)
      .findFirst()
      .map(key -> context.getAllHeaders().get(key)).ifPresent(values -> allHeaders.put("Accept-Language", values));
    return allHeaders;
  }
}
