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

import com.stun4j.guid.core.utils.Utils;

public class LocalZkGuidUsage {

  public static void main(String[] args) throws Exception {
    // LocalGuid solo = LocalGuid.init(0, 1, 15, 1, 1, 6, false);
    LocalGuid guid = LocalZkGuid.init("localhost:2181", null, 15, 1, 1, 6, false, null);
    long id = guid.next();
    System.out.println(id);

    long id2 = LocalGuid.instance().next();
    System.out.println(id2);

    new Thread(() -> {
      while (true) {
        Utils.sleepSeconds(5);
        System.out.println("background id generating: " + LocalGuid.instance().next());
      }
    }).start();

    // keep work process running;
    Thread.sleep(Integer.MAX_VALUE);
  }

}
