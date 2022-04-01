package com.stun4j.guid;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Date;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.common.collect.Sets;
import com.stun4j.guid.utils.Strings;
import com.stun4j.guid.utils.Utils.Pair;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LocalGuidTest {

  @Before
  public void mockReset() {
    try {
      LocalGuid.reset();
    } catch (Throwable e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  @Test
  public void _1_basicSingleton() {
    LocalGuid.init(1, 1);
    assertThat(LocalGuid.instance().getDatacenterId()).isEqualTo(1);
    assertThat(LocalGuid.instance().getWorkerId()).isEqualTo(1);

    // assert the old singleton preserved
    LocalGuid.init(Pair.of(2, 2));
    LocalGuid instance;
    assertThat(instance = LocalGuid.instance()).isSameAs(LocalGuid.instance());
    assertThat(instance).isSameAs(LocalGuid.instance());
    assertThat(LocalGuid.instance().getDatacenterId()).isEqualTo(1);
    assertThat(LocalGuid.instance().getWorkerId()).isEqualTo(1);
  }
  // FIXME mj:cross-classloader,multi-thread tests...

  @Test
  public void _2_extract() {
    LocalGuid guid = LocalGuid.init(0, 0);
    // System.out.println(guid.next());
    Date date = new Date();
    // System.out.println(date);
    // System.out.println(guid.next());
    long id = guid.from(date);
    // System.out.println(id = guid.from(date));
    long ts = guid.getTimeMsFromId(id);
    assertThat(new Date(ts)).isEqualTo(date);
  }

  @Test
  public void _3_highConcurrencySingleton_instance() throws Exception {
    LocalGuid.init(0, 0);
    int n = 800;
    CyclicBarrier barrier = new CyclicBarrier(n);
    CountDownLatch latch = new CountDownLatch(n);
    Set<LocalGuid> set = Sets.newConcurrentHashSet();

    for (int i = 0; i < n; i++) {
      new Thread(() -> {
        try {
          barrier.await();
          LocalGuid instance = LocalGuid.instance();
          set.add(instance);
        } catch (Throwable e) {
          e.printStackTrace();
        } finally {
          latch.countDown();
        }
      }).start();
    }
    latch.await();
    assertThat(set).hasSize(1);
  }

  @Test
  public void _0_highConcurrencySingleton_init() throws Exception {
    int concurrencyLvl = 500;
    int expectedInitTimes = 100;
    Set<LocalGuid> set = Sets.newConcurrentHashSet();
    for (int r = 0; r < expectedInitTimes; r++) {
      CyclicBarrier barrier = new CyclicBarrier(concurrencyLvl);
      CountDownLatch latch = new CountDownLatch(concurrencyLvl);
      for (int i = 0; i < concurrencyLvl; i++) {
        int idx = i;
        new Thread(() -> {
          try {
            barrier.await();
            LocalGuid instance = LocalGuid.init(idx & 31, idx & 31);
            set.add(instance);
          } catch (Throwable e) {
            e.printStackTrace();
          } finally {
            latch.countDown();
          }
        }).start();
      }
      latch.await();
      LocalGuid.reset();
    }
    assertThat(set).hasSize(expectedInitTimes);

    Thread.sleep(1000);
    // String[] cmd = { "/bin/sh", "-c", "grep 'INFO (' logs-test/info.log|wc -l" };
    // Process proc = Runtime.getRuntime().exec(cmd);
    // proc.waitFor();
    // try (BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF-8"))) {
    // StringBuilder builder = new StringBuilder();
    // String line;
    // while ((line = in.readLine()) != null) {
    // builder.append(line);
    // break;
    // }
    // assertThat(builder.toString().trim()).containsSequence(expectedInitTimes + "");
    // } finally {
    // if (proc != null) {
    // proc.destroy();
    // }
    // }
    try (Scanner scanner = new Scanner(new File("logs-test/info.log")).useDelimiter("\n")) {
      int times = 0;
      while (scanner.hasNext()) {
        if (scanner.next().contains("INFO (")) {
          times++;
        }
      }
      assertThat(times).isEqualTo(expectedInitTimes);
    }

  }

  @Test
  public void _4_basicGuid_next() throws Exception {
    LocalGuid.init(0, 0);
    int threadNum = 800;
    int guidGenerateTimesPerThread = 100;
    CyclicBarrier barrier = new CyclicBarrier(threadNum);
    CountDownLatch latch = new CountDownLatch(threadNum);
    Set<Long> set = Sets.newConcurrentHashSet();
    for (int i = 0; i < threadNum; i++) {
      new Thread(() -> {
        try {
          barrier.await();
          LocalGuid instance = LocalGuid.instance();
          for (int j = 0; j < guidGenerateTimesPerThread; j++) {
            set.add(instance.next());
          }
        } catch (Throwable e) {
          e.printStackTrace();
        } finally {
          latch.countDown();
        }
      }).start();
    }
    latch.await();
    assertThat(set).hasSize(threadNum * guidGenerateTimesPerThread);
  }

  @Test
  public void _5_highConcurrencyGuid_next_underMockHighFrequencyReconnect() throws Exception {
    int threadNum = 800;
    int guidGenerateTimesPerThread = 100;
    Set<Long> set = Sets.newConcurrentHashSet();
    Set<LocalGuid> set2 = Sets.newConcurrentHashSet();
    CountDownLatch latch = new CountDownLatch(threadNum);
    set2.add(LocalGuid.init(0, 0));
    for (int i = 0; i < threadNum; i++) {
      int idx = i + 1;
      new Thread(() -> {
        try {
          String binStr = Strings.leftPad(Integer.toBinaryString(idx), 10, "0");
          String highAsDatacenterId = binStr.substring(0, 5);
          String lowAsWorkerId = binStr.substring(5, 10);
          Integer datacenterId = Integer.valueOf(highAsDatacenterId, 2);
          Integer workerId = Integer.valueOf(lowAsWorkerId, 2);

          LocalGuid mutex;
          LocalGuid instanceCached;
          synchronized (mutex = LocalGuid.instance()) {
            mutex.reset(Pair.of(datacenterId, workerId));
            instanceCached = LocalGuid.init(datacenterId, workerId);
            assertThat(mutex.getDatacenterId()).isEqualTo(datacenterId.intValue());
            assertThat(mutex.getWorkerId()).isEqualTo(workerId.intValue());
            assertThat(instanceCached.getDatacenterId()).isEqualTo(datacenterId.intValue());
            assertThat(instanceCached.getWorkerId()).isEqualTo(workerId.intValue());
          }
          set2.add(mutex);
          set2.add(instanceCached);
          for (int j = 0; j < guidGenerateTimesPerThread; j++) {
            set.add(instanceCached.next());
          }
        } catch (Throwable e) {
          e.printStackTrace();
        } finally {
          latch.countDown();
        }
      }).start();
    }
    latch.await();
    assertThat(set).hasSize(threadNum * guidGenerateTimesPerThread);
    assertThat(set2).hasSize(1);
  }

  @Test
  public void _6_basicGuid_uuid() throws Exception {
    int threadNum = 800;
    int guidGenerateTimesPerThread = 1000;
    CyclicBarrier barrier = new CyclicBarrier(threadNum);
    CountDownLatch latch = new CountDownLatch(threadNum);
    Set<String> set = Sets.newConcurrentHashSet();
    for (int i = 0; i < threadNum; i++) {
      new Thread(() -> {
        try {
          barrier.await();
          for (int j = 0; j < guidGenerateTimesPerThread; j++) {
            set.add(LocalGuid.uuid());
          }
        } catch (Throwable e) {
          e.printStackTrace();
        } finally {
          latch.countDown();
        }
      }).start();
    }
    latch.await();
    assertThat(set).hasSize(threadNum * guidGenerateTimesPerThread);
  }
}
