package com.stun4j.guid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.Before;
import org.junit.Test;

public class LocalGuidErrorTest {

  @Before
  public void mockReset() {
    try {
      LocalGuid.instance().reset();
    } catch (Exception e) {
    }
  }

  @Test
  public void basic() {
    long idBits = 5L;
    long maxId = -1L ^ (-1L << idBits);
    assertThat(maxId).isEqualTo(31);
    String initMsg = "local-guid must be initialized in the very begining";
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> LocalGuid.instance())
        .withMessage(initMsg);
    // datacenterId range protect
    String msg = "datacenterId can't be greater than " + maxId + " or less than 0";
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> LocalGuid.init(-1, 0)).withMessage(msg);
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> LocalGuid.init(32, 0)).withMessage(msg);
    // workerId range protect
    msg = "workerId can't be greater than " + maxId + " or less than 0";
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> LocalGuid.init(0, -1)).withMessage(msg);
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> LocalGuid.init(0, 32)).withMessage(msg);

    // a safe post check
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> LocalGuid.instance())
        .withMessage(initMsg);
  }
  // FIXME mj:cross-classloader,multi-thread tests...
}
