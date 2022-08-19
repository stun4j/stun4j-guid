/*
 * Copyright 2020-? the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stun4j.guid.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.stun4j.guid.core.utils.Utils;

public class LocalGuidBenchmark {
  static final ExecutorService E = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
      Runtime.getRuntime().availableProcessors(), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1024),
      new ThreadPoolExecutor.CallerRunsPolicy());

  public static void main(String[] args) {
    byte mode = 0;// 0:single thread 1:multi-thread
    LocalGuid guid = LocalGuid.init(0, 0);
    // warm round
    for (int i = 0; i < 10_0000; i++) {
      guid.next();
    }
    // benchmark round
    MetricRegistry registry = new MetricRegistry();
    ConsoleReporter reporter = ConsoleReporter.forRegistry(registry).build();
    Meter meter = registry.meter("qps");
    reporter.start(0, 250, TimeUnit.MILLISECONDS);
    int round = 5000_0000;
    long start = System.currentTimeMillis();
    // start
    if (mode == 0) {
      for (int i = 0; i < round; i++) {
        // guid.next();
        LocalGuid.instance().next();
        meter.mark();
      }
    } else {
      for (int i = 0; i < round; i++) {
        E.execute(() -> {
          // guid.next();
          LocalGuid.instance().next();
        });
        meter.mark();
      }
    }
    System.out.println(System.currentTimeMillis() - start);
    Utils.sleepMs(300);
  }
}
