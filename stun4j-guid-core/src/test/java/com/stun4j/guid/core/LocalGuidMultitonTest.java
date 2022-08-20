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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LocalGuidMultitonTest {
  static {
    LocalGuid._show_initialization_report = false;
  }

  @BeforeClass
  public static void beforeClass() {
    LocalGuidMultiton._enabled = true;
  }

  @AfterClass
  public static void afterClass() {
    LocalGuidMultiton._enabled = false;
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
  public void _1_basic() {
    LocalGuid solo = LocalGuid.init(15, 1, 15, 4, 4, 3, true);
    assertThat((solo.next() + "").length()).isEqualTo(15);
    LocalGuid solo2 = LocalGuid.init(15, 2, 15, 4, 4, 3, true);// to the solo,new initialization was ignored
    LocalGuid solo3 = LocalGuidMultiton.instance(15, 1, 15, 4, 4, 3, true);
    assertThat(solo).isSameAs(solo2).isSameAs(solo3);
    LocalGuid solo4 = LocalGuidMultiton.instance(15, 2, 15, 4, 4, 3, true);// once initialized,the pattern will be saved
    assertThat(solo).isNotSameAs(solo4);
    assertThat((solo4.next() + "").length()).isEqualTo(15);

    // Slightly different pattern with solo4,which will generate a new guid-instance
    LocalGuid solo5 = LocalGuid.init(15, 2, 15, 4, 4, 3, false);
    assertThat(solo).isSameAs(solo5);// still the most early solo
    LocalGuid solo6 = LocalGuidMultiton.instance(15, 2, 15, 4, 4, 3, false);// this is the new instance
    assertThat(solo6).isNotNull();
    assertThat(solo4).isNotSameAs(solo6);
    assertThat(solo).isNotSameAs(solo6);

    // Another way check solo(with dcId,wkId omitted)
    LocalGuid soloAgain = LocalGuidMultiton.instance(15, 4, 4, 3, true);
    assertThat(solo).isSameAs(soloAgain);

    LocalGuid.init(15, 1, 16, 5, 5, 5, false);// try register a new pattern
    LocalGuid solo7 = LocalGuidMultiton.instance(16, 5, 5, 5, false);// instance by the new pattern(with dcId,wkId
                                                                     // omitted)
    assertThat(solo7).isNotSameAs(solo).isNotNull();
  }

}
