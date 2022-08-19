package com.stun4j.guid.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.NumberFormat;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LocalGuidMultiTest {
  static {
    LocalGuid._show_initialization_report = false;
  }

  @BeforeClass
  public static void beforeClass() {
    LocalGuidMulti._enabled = true;
  }

  @AfterClass
  public static void afterClass() {
    LocalGuidMulti._enabled = false;
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
  public void _1_15d_multi() {
    LocalGuid solo = LocalGuid.init(15, 1, 15, 4, 4, 3, true);
    assertThat((solo.next() + "").length()).isEqualTo(15);
    LocalGuid solo2 = LocalGuid.init(15, 2, 15, 4, 4, 3, true);// to the solo,new initialization was ignored
    LocalGuid solo3 = LocalGuidMulti.instance(15, 1, 15, 4, 4, 3, true);
    assertThat(solo).isSameAs(solo2).isSameAs(solo3);
    LocalGuid solo4 = LocalGuidMulti.instance(15, 2, 15, 4, 4, 3, true);// once initialized,the pattern will be saved
    assertThat(solo).isNotSameAs(solo4);
    assertThat((solo4.next() + "").length()).isEqualTo(15);

    // Slightly different pattern with solo4,which will generate a new guid-instance
    LocalGuid solo5 = LocalGuid.init(15, 2, 15, 4, 4, 3, false);
    assertThat(solo).isSameAs(solo5);// still the most early solo
    LocalGuid solo6 = LocalGuidMulti.instance(15, 2, 15, 4, 4, 3, false);// this is the new instance
    assertThat(solo4).isNotSameAs(solo6);
    assertThat(solo).isNotSameAs(solo6);
  }

}
