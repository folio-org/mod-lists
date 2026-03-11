package org.folio.list.services;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs code in an execution context using the current tenantId with no other headers,
 * simulating a system user's context. Eureka uses a lack of token/etc from us here to
 * determine that things are running as a system user and will inject the token/etc at
 * the routing level.
 */
@Log4j2
@Service
@AllArgsConstructor
public class RunAsSystemUserService {

  private final FolioExecutionContext outerContext;

  /** Executes a given job in scope of system user, synchronously. */
  public <T> T executeSystemUserScoped(String tenantId, Supplier<T> job) {
    return systemUserExecutionContext(tenantId).execute(job::get);
  }

  /** Executes a given job in scope of system user, asynchronously. */
  @Async
  public <T> CompletableFuture<T> executeAsyncSystemUserScoped(String tenantId, Supplier<T> job) {
    return CompletableFuture.completedFuture(systemUserExecutionContext(tenantId).execute(job::get));
  }

  private FolioExecutionContext systemUserExecutionContext(String tenantId) {
    log.info("Creating system user execution context for tenant {}", tenantId);
    log.info("Extracted URL from outer context: {}", outerContext.getOkapiUrl());
    log.info("Running with module metadata {}", outerContext.getFolioModuleMetadata());
    return new DefaultFolioExecutionContext(
      outerContext.getFolioModuleMetadata(),
      Map.ofEntries(
        Map.entry(XOkapiHeaders.URL, Collections.singleton(outerContext.getOkapiUrl())),
        Map.entry(XOkapiHeaders.TENANT, Collections.singleton(tenantId))
      )
    );
  }
}
