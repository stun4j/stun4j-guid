package com.stun4j.guid;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.stun4j.guid.utils.Pair;

public class LocalGuidTest {

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
  //FIXME mj:cross-classloader,multi-thread tests...
}
