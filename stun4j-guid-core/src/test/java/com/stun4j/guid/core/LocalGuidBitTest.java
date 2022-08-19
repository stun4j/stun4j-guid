package com.stun4j.guid.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LocalGuidBitTest {
  static {
    LocalGuid._show_initialization_report = false;
  }

  static class MockLocalGuid extends LocalGuid {

//    public MockLocalGuid(int digits, long datacenterIdBits, long workerIdBits, long seqBits,
//        boolean fixedDigitsEnabled) {
//      super(digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled);
//    }

    public MockLocalGuid(int datacenterId, int workerId, int digits, long datacenterIdBits, long workerIdBits,
        long seqBits, boolean fixedDigitsEnabled) {
      super(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled);
      // TODO Auto-generated constructor stub
    }

    @Override
    protected long currentTimeMs() {
      // return super.currentTimeMs();
      return this.getEpoch() + this.getMaxDeltaMs() + 1;
    }

  }

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
  public void _1_15d_fixed() {
    LocalGuid solo = LocalGuid.init(15, 1, 15, 4, 4, 3, true);
    assertThat((solo.next() + "").length()).isEqualTo(15);
  }

  @Test
  public void _2_15d_fixed_min() {
    LocalGuid solo = LocalGuid.init(15, 1, 15, 4, 4, 3, true, MockLocalGuid.class);
    long id = solo.next();
    System.out.println(new Date(solo.getTimeMsFromId(id)));
    assertThat((id + "").length()).isEqualTo(15);
  }

  @Test
  public void _3_15d_nonfixed() {
    LocalGuid solo = LocalGuid.init(7, 1, 15, 3, 4, 3, false);
    long id = solo.next();
    System.out.println(new Date(solo.getTimeMsFromId(id)));
    assertThat((id + "").length()).isEqualTo(14);
  }

}
