package org.folio.list.services;

import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@Log4j2
public class AppShutdownService {

  private final Set<ShutdownTask> shutdownTasks;

  public AppShutdownService() {
    this.shutdownTasks = Collections.synchronizedSet(new LinkedHashSet<>());
  }

  /**
   * Register a shutdown task that will be executed on app shutdown.
   * This method returns an AutoCloseable. When it is closed, the shutdown task is deregistered and will not run on
   * shutdown.
   * This method is intended to be used with try-with-resources or otherwise closed when clean up is no longer needed.
   */
  public ShutdownTask registerShutdownTask(FolioExecutionContext context, Runnable task, String taskName) {
    // Use FolioExecutionContext.getInstance(), so that we have a real instance and not a proxy object that doesn't work
    // during app shutdown
    return new ShutdownTask((FolioExecutionContext) context.getInstance(), task, taskName);
  }

  @PreDestroy
  public void preDestroy() {
    log.info("Running shutdown tasks prior to app shutdown");
    synchronized (shutdownTasks) {
      for (var shutdownTask : shutdownTasks) {
        log.info("Running shutdown task: {}", shutdownTask.name);
        // Set the context here, to put it into the ThreadLocals used during for DB access.
        try (var context = setExecutionContext(shutdownTask.context)) {
          shutdownTask.task.run();
        } catch (Exception e) {
          // Just log the error so that other shutdown tasks can attempt to run. The app is shutting down, so we can't
          // really recover at this point anyway ¯\_(ツ)_/¯
          log.error("Error running shutdown task during app shutdown", e);
        }
      }
    }
  }

  public class ShutdownTask implements AutoCloseable {
    private final FolioExecutionContext context;
    private final Runnable task;
    private final String name;

    private ShutdownTask(FolioExecutionContext context, Runnable task, String name) {
      this.context = context;
      this.task = task;
      this.name = name;
      shutdownTasks.add(this);
    }

    @Override
    public void close() {
      shutdownTasks.remove(this);
    }
  }

  // Visible for testing
  FolioExecutionContextSetter setExecutionContext(FolioExecutionContext context) {
    return new FolioExecutionContextSetter(context);
  }
}
