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

import org.junit.Test;

import com.stun4j.guid.core.utils.Strings;

public class ZkGuidNodeTest {
  @Test
  public void fastTransTest() {
    for (int i = 1024; i > 0; i--) {
      int rtnNodeId = i;
      String binStr = Strings.leftPad(Integer.toBinaryString(--rtnNodeId), 10, "0");
      String lowAsDatacenterId = binStr.substring(0, 5);
      String highAsWorkerId = binStr.substring(5, 10);
      Integer datacenterId = Integer.valueOf(lowAsDatacenterId, 2);
      Integer workerId = Integer.valueOf(highAsWorkerId, 2);
      System.out.format("rtnNodeId=%s, datacenterId=%s, workerId=%s", rtnNodeId, datacenterId, workerId);
      rtnNodeId += 1;
      int dcId = (--rtnNodeId) >> 5;
      int wkId = (int)(rtnNodeId & ~(-1L << 5L));
      System.out.format(" | rtnNodeId=%s, dcId=%s, wkId=%s\n", rtnNodeId, dcId, wkId);
      assertThat(datacenterId).isEqualTo(dcId);
      assertThat(workerId).isEqualTo(wkId);
    }

  }
}
