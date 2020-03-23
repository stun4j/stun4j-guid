package com.stun4j.guid;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

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
//    System.out.println(date);
//    System.out.println(guid.next());
    long id= guid.from(date);
//    System.out.println(id = guid.from(date));
    long ts = guid.getTimeMsFromId(id);
    assertThat(new Date(ts)).isEqualTo(date);
  }
}
