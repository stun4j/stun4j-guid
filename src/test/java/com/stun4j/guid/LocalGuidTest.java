package com.stun4j.guid;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.stun4j.guid.utils.Pair;

public class LocalGuidTest {

  @Before
  public void mockReset() {
    try {
      LocalGuid.instance().reset();
    } catch (Exception e) {
    }
  }

  @Test
  public void basicSingleton() {
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
  public void extract() {
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
  public void highConcurrencySingleton() throws Exception {
    LocalGuid.init(0, 0);
    int n = 500;
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
}
