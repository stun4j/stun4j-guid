package com.stun4j.guid;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.common.collect.Sets;
import com.stun4j.guid.utils.Pair;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LocalGuidTest {

  @Before
  public void mockReset() {
    try {
      LocalGuid.instance().reset();
    } catch (Exception e) {
    }
  }

  @Test
  public void _1_basicSingleton() {
    LocalGuid.init(1, 1);
    assertThat(1).isEqualTo(LocalGuid.instance().getDatacenterId());
    assertThat(1).isEqualTo(LocalGuid.instance().getWorkerId());

    // assert the old singleton preserved
    LocalGuid.init(Pair.of(2, 2));
    LocalGuid instance;
    assertThat(instance = LocalGuid.instance()).isSameAs(LocalGuid.instance());
    assertThat(instance).isSameAs(LocalGuid.instance());
    assertThat(1).isEqualTo(LocalGuid.instance().getDatacenterId());
    assertThat(1).isEqualTo(LocalGuid.instance().getWorkerId());
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
          latch.countDown();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }).start();
    }
    latch.await();
    assertThat(set).hasSize(1);
  }

  @Test
  public void _0_highConcurrencySingleton_init() throws Exception {
    int ConcurrencyLvl = 500;
    int expectedInitTimes = 100;
    Set<LocalGuid> set = Sets.newConcurrentHashSet();
    for (int r = 0; r < expectedInitTimes; r++) {
      CyclicBarrier barrier = new CyclicBarrier(ConcurrencyLvl);
      CountDownLatch latch = new CountDownLatch(ConcurrencyLvl);
      for (int i = 0; i < ConcurrencyLvl; i++) {
        int idx = i;
        new Thread(() -> {
          try {
            barrier.await();
            LocalGuid instance = LocalGuid.init(idx & 31, idx & 31);
            set.add(instance);
            latch.countDown();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }).start();
      }
      latch.await();
      LocalGuid.instance().reset();
    }
    assertThat(set).hasSize(1);

    Thread.sleep(1000);
    String[] cmd = { "/bin/sh", "-c", "grep 'INFO (' logs-test/info.log|wc -l" };
    Process proc = Runtime.getRuntime().exec(cmd);
    proc.waitFor();
    try (BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF-8"))) {
      StringBuilder builder = new StringBuilder();
      String line;
      while ((line = in.readLine()) != null) {
        builder.append(line);
        break;
      }
      assertThat(builder.toString().trim()).containsSequence(expectedInitTimes + "");
    } finally {
      if (proc != null) {
        proc.destroy();
      }
    }
  }
}
