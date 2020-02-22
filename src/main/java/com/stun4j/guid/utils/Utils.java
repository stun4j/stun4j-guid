/*-
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*-
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stun4j.guid.utils;

import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.function.Supplier;

/** @author Jay Meng */
public final class Utils {
  private static final int NANOS_PER_MS = 1000 * 1000;
  private static final int NANOS_PER_SECONDS = 1000 * NANOS_PER_MS;

  public static void sleepMs(int ms) {
    LockSupport.parkNanos(ms * NANOS_PER_MS);
  }

  public static void sleepSeconds(int seconds) {
    LockSupport.parkNanos(seconds * NANOS_PER_SECONDS);
  }

  public static <T> Pair<Long, T> timeAwareRun(Supplier<T> supplier) {
    long start = System.currentTimeMillis();
    T rtn = supplier.get();
    return Pair.of(System.currentTimeMillis() - start, rtn);
  }

  public static <T, R> Pair<Long, R> timeAwareRun(Function<T, R> fn, T arg) {
    long start = System.currentTimeMillis();
    R rtn = fn.apply(arg);
    return Pair.of(System.currentTimeMillis() - start, rtn);
  }

  private Utils() {
  }
}
