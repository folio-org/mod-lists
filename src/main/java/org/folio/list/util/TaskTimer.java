package org.folio.list.util;

import org.folio.list.services.refresh.TimedStage;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public class TaskTimer {
  Map<TimedStage, Stopwatch> stopwatches = new EnumMap<>(TimedStage.class);

  public void start(TimedStage task) {
    if (stopwatches.get(task) != null) {
      throw new IllegalStateException("Attempted to start timer for task " + task + " but it was already started");
    }
    stopwatches.put(task, new Stopwatch());
  }

  public void stop(TimedStage task) {
    var stopwatch = stopwatches.get(task);
    if (stopwatch == null) {
      throw new IllegalStateException("Attempted to stop timer for task " + task + " but it was not started");
    }
    stopwatch.stop = Instant.now();
  }

  public Duration getElapsedTime(TimedStage task) {
    var stopwatch = stopwatches.get(task);
    return stopwatch == null ? null : stopwatch.getElapsedTime();
  }

  public <R> R time(TimedStage taskKey, Supplier<R> task) {
    start(taskKey);
    var retVal = task.get();
    stop(taskKey);
    return retVal;
  }

  public void time(TimedStage taskKey, Runnable task) {
    time(taskKey, () -> {
      task.run();
      return null;
    });
  }

  private static class Stopwatch {
    private final Instant start;
    private Instant stop;

    private Stopwatch(Instant start) {
      this.start = start;
    }

    private Stopwatch() {
      this(Instant.now());
    }

    public Duration getElapsedTime() {
      if (stop == null) {
        return Duration.between(start, Instant.now());
      }
      return Duration.between(start, stop);
    }

    public void stop() {
      this.stop = Instant.now();
    }
  }

}
