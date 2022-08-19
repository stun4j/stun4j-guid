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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.lang.reflect.InvocationTargetException;

import org.junit.Before;
import org.junit.Test;

public class LocalGuidErrorTest {

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
  public void basic() {
    long idBits = 5L;
    // long maxId = -1L ^ (-1L << idBits);
    long maxId = ~(-1L << idBits);
    assertThat(maxId).isEqualTo(31);
    String initMsg = "The local-guid must be initialized in the very begining";
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> LocalGuid.instance())
        .withMessage(initMsg);
    // datacenterId range protect
    String msg = "The datacenterId can't be greater than " + maxId + " or less than 0";
    // assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> LocalGuid.init(-1,
    // 0)).withMessage(msg);
    // assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> LocalGuid.init(32,
    // 0)).withMessage(msg);
    assertThatExceptionOfType(InvocationTargetException.class).isThrownBy(() -> LocalGuid.init(-1, 0))
        .withCauseExactlyInstanceOf(IllegalArgumentException.class).withStackTraceContaining(msg);
    assertThatExceptionOfType(InvocationTargetException.class).isThrownBy(() -> LocalGuid.init(32, 0))
        .withCauseExactlyInstanceOf(IllegalArgumentException.class).withStackTraceContaining(msg);
    // workerId range protect
    msg = "The workerId can't be greater than " + maxId + " or less than 0";
    // assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> LocalGuid.init(0,
    // -1)).withMessage(msg);
    // assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> LocalGuid.init(0,
    // 32)).withMessage(msg);
    assertThatExceptionOfType(InvocationTargetException.class).isThrownBy(() -> LocalGuid.init(0, -1))
        .withCauseExactlyInstanceOf(IllegalArgumentException.class).withStackTraceContaining(msg);
    assertThatExceptionOfType(InvocationTargetException.class).isThrownBy(() -> LocalGuid.init(0, 32))
        .withCauseExactlyInstanceOf(IllegalArgumentException.class).withStackTraceContaining(msg);

    // a safe post check
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> LocalGuid.instance())
        .withMessage(initMsg);
  }
  // FIXME mj:cross-classloader,multi-thread tests...
}
