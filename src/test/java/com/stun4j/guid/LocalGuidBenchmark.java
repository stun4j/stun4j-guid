package com.stun4j.guid;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public class LocalGuidBenchmark {
  static final ExecutorService E = Executors.newWorkStealingPool();

  public static void main(String[] args) throws Exception {
    LocalGuid guid = LocalGuid.init(0, 0);
    // warm round
    for (int i = 0; i < 10_0000; i++) {
      guid.next();
    }
    // start benchmark
    MetricRegistry registry = new MetricRegistry();
    ConsoleReporter reporter = ConsoleReporter.forRegistry(registry).build();
    // Slf4jReporter reporter = Slf4jReporter.forRegistry(registry).build();
    Meter meter = registry.meter("tps");
    long start = System.currentTimeMillis();
    reporter.start(0, 250, TimeUnit.MILLISECONDS);
    // start
    for (int i = 0; i < 1000_0000; i++) {
      E.execute(() -> {
        guid.next();
      });
      meter.mark();
    }
    System.out.println(System.currentTimeMillis() - start);
  }
}
