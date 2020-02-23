package com.stun4j.guid;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.stun4j.guid.utils.Utils;

public class LocalGuidBenchmark {
  // static final ExecutorService E = Executors.newWorkStealingPool();
  static final ExecutorService E = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
      Runtime.getRuntime().availableProcessors(), 0L, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue<Runnable>(1000), new ThreadPoolExecutor.CallerRunsPolicy());

  public static void main(String[] args) {
    LocalGuid guid = LocalGuid.init(0, 0);
    // warm round
    for (int i = 0; i < 10_0000; i++) {
      guid.next();
    }
    // benchmark round
    MetricRegistry registry = new MetricRegistry();
    ConsoleReporter reporter = ConsoleReporter.forRegistry(registry).build();
    Meter meter = registry.meter("tps");
    long start = System.currentTimeMillis();
    reporter.start(0, 250, TimeUnit.MILLISECONDS);
    // start
    for (int i = 0; i < 5000_0000; i++) {
      E.execute(() -> {
        guid.next();
      });
      meter.mark();
    }
    System.out.println(System.currentTimeMillis() - start);
    Utils.sleepSeconds(10);
  }
}
