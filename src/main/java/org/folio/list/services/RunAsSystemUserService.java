package org.folio.list.services;

import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.AllArgsConstructor;

/**
 * Runs code in an execution context using the current tenantId with only safe non-auth headers,
 * simulating a system user's context. Eureka uses a lack of token/etc from us here to
 * determine that things are running as a system user and will inject the token/etc at
 * the routing level.
 */
@Service
@AllArgsConstructor
public class RunAsSystemUserService {

  private final AsyncTaskExecutor taskExecutor;
  private final FolioExecutionContext outerContext;

  /** Executes a given job in scope of system user, synchronously. MUST be called from request thread. */
  public <T> T executeSystemUserScoped(String tenantId, Supplier<T> job) {
    return systemUserExecutionContext(tenantId).execute(job::get);
  }

  /**
   * Creates a thread-safe executor to run system user tasks with, later.
   * This allows callers to grab this in the main request thread and use it later
   * in async threads without needing to worry about the execution context themselves.
   */
  public <T> Function<Supplier<T>, T> prepareExecutorWithSystemUserContext(String tenantId) {
    FolioExecutionContext derivedContext = systemUserExecutionContext(tenantId);
    return job -> derivedContext.execute(job::get);
  }

  /**
   * Executes a given job in scope of system user, asynchronously.
   * This MUST be called from request thread; if this is not possible then use
   * {@link #prepareExecutorWithSystemUserContext(String)}
   */
  public <T> CompletableFuture<T> executeAsyncSystemUserScoped(String tenantId, Supplier<T> job) {
    Function<Supplier<T>, T> systemUserExecutor = prepareExecutorWithSystemUserContext(tenantId);
    return taskExecutor.submitCompletable(() -> systemUserExecutor.apply(job));
  }

  private FolioExecutionContext systemUserExecutionContext(String tenantId) {
    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.URL, Collections.singleton(outerContext.getOkapiUrl()));
    headers.put(XOkapiHeaders.TENANT, Collections.singleton(tenantId));
    copyAcceptLanguageHeader(headers);

    return new DefaultFolioExecutionContext(
      outerContext.getFolioModuleMetadata(),
      headers
    );
  }

  private void copyAcceptLanguageHeader(Map<String, Collection<String>> headers) {
    outerContext.getAllHeaders().forEach((key, value) -> {
      if (HttpHeaders.ACCEPT_LANGUAGE.equalsIgnoreCase(key)) {
        headers.put(HttpHeaders.ACCEPT_LANGUAGE, List.copyOf(value));
      }
    });
  }
}
