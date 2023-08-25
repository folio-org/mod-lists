package org.folio.list.services;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AppShutdownServiceTest {

  @Test
  void shutdownTasksShouldNotRunWithoutShutdown() {
    var context = mock(FolioExecutionContext.class);
    AtomicBoolean taskRan = new AtomicBoolean(false);
    Runnable task = () -> taskRan.set(true);
    var appShutdownService = new AppShutdownService();

    try (var shutdownTask = appShutdownService.registerShutdownTask(context, task, "Set taskRan = true")) {
      assertThat(taskRan).isFalse();
    }
    assertThat(taskRan).isFalse();
  }

  @Test
  void shutdownTasksShouldRunWithShutdown() {
    var context = mock(FolioExecutionContext.class);
    AtomicBoolean taskRan = new AtomicBoolean(false);
    Runnable task = () -> taskRan.set(true);
    // Inject a mock context setter, to avoid all the extra DB stuff the real one does
    var appShutdownService = new AppShutdownService() {
      @Override
      FolioExecutionContextSetter setExecutionContext(FolioExecutionContext context) {
        return mock(FolioExecutionContextSetter.class);
      }
    };

    try (var shutdownTask = appShutdownService.registerShutdownTask(context, task, "Set taskRan = true")) {
      assertThat(taskRan).isFalse();
      // Simulate the shutdown that Spring would normally trigger for us. Do it from inside the try block so that the
      // shutdown happens while the task is still active
      appShutdownService.preDestroy();
    }
    assertThat(taskRan).isTrue();
  }

  @Test
  void completedTasksShouldNotRunOnShutdown() {
    var context = mock(FolioExecutionContext.class);
    AtomicBoolean taskRan = new AtomicBoolean(false);
    Runnable task = () -> taskRan.set(true);
    var appShutdownService = new AppShutdownService();

    try (var shutdownTask = appShutdownService.registerShutdownTask(context, task, "Set taskRan = true")) {}
    // Simulate the shutdown that Spring would normally trigger for us. Do it from outside the try block so that the
    // shutdown happens after the task has been removed from the shutdown tasks
    appShutdownService.preDestroy();
    assertThat(taskRan).isFalse();
  }

  @Test
  void shouldUsesRealInstanceOfFolioExecutionContext() {
    var appShutdownService = new AppShutdownService();
    var context = mock(FolioExecutionContext.class);

    try (var task = appShutdownService.registerShutdownTask(context, () -> {}, "No-op")) {
      verify(context, times(1)).getInstance();
    }
  }
}
