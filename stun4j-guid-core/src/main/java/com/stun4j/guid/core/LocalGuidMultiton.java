/*
 * Copyright 2022-? the original author or authors.
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

import static com.stun4j.guid.core.LocalGuid.logSuccessfullyInitialized;
import static com.stun4j.guid.core.utils.Strings.lenientFormat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stun4j.guid.core.utils.Exceptions;

/**
 * LocalGuid implemented by applying Multiton(Pattern).
 * @author Jay Meng
 */
public class LocalGuidMultiton {
  private static final Logger LOG = LoggerFactory.getLogger(LocalGuidMultiton.class);

  public static volatile boolean _enabled = false;
  public static boolean _auto_register_enabled = true;

  private static final Map<String, LocalGuid> REG = new ConcurrentHashMap<>();
  private static final Map<Class<? extends LocalGuid>, String> RESERVED_KEY_SUFFIX = new HashMap<>();
  static {
    RESERVED_KEY_SUFFIX.put(LocalGuid.class, "0");
  }

  static LocalGuid putIfAbsent(int digits, int datacenterIdBits, int workerIdBits, int seqBits,
      boolean fixedDigitsEnabled, Class<? extends LocalGuid> guidClz, LocalGuid instance) {
    int datacenterId;
    int workerId;
    synchronized (instance) {
      datacenterId = (int)instance.getDatacenterId();
      workerId = (int)instance.getWorkerId();
    }
    return putIfAbsent(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled,
        guidClz, instance);
  }

  static LocalGuid putIfAbsent(int datacenterId, int workerId, int digits, int datacenterIdBits, int workerIdBits,
      int seqBits, boolean fixedDigitsEnabled, Class<? extends LocalGuid> guidClz, LocalGuid instance) {
    String key = keyBy(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled,
        guidClz);
    LocalGuid old = REG.putIfAbsent(key, instance);
    return old;
  }

  public static LocalGuid instance(int digits, int datacenterIdBits, int workerIdBits, int seqBits,
      boolean fixedDigitsEnabled) {
    int datacenterId;
    int workerId;
    LocalGuid instance;
    synchronized (instance = LocalGuid.instance()) {
      datacenterId = (int)instance.getDatacenterId();
      workerId = (int)instance.getWorkerId();
    }
    return instance(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled);
  }

  static LocalGuid instance(int datacenterId, int workerId, int digits, int datacenterIdBits, int workerIdBits,
      int seqBits, boolean fixedDigitsEnabled) {
    return instance(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled,
        LocalGuid.class);
  }

  static LocalGuid instance(int datacenterId, int workerId, int digits, int datacenterIdBits, int workerIdBits,
      int seqBits, boolean fixedDigitsEnabled, Class<? extends LocalGuid> guidClz) {
    String key = keyBy(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled,
        guidClz);
    if (_enabled && _auto_register_enabled) {
      // LocalGuid.init(...);//works but bad performance
      return REG.computeIfAbsent(key, k -> {
        try {
          LocalGuid instance = guidClz
              .getDeclaredConstructor(int.class, int.class, int.class, int.class, int.class, int.class, boolean.class)
              .newInstance(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled);
          logSuccessfullyInitialized(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits,
              fixedDigitsEnabled, LOG);
          return instance;
        } catch (Exception e) {
          Exceptions.sneakyThrow(e);
        }
        return null;
      });
    }
    return Optional.ofNullable(REG.get(key)).orElseThrow(() -> new IllegalStateException(lenientFormat(
        "The local-guid#pattern=%s must be registered first > Consider pre-registering this pattern by using 'LocalGuid#init'",
        key)));

  }

  private static String keyBy(int datacenterId, int workerId, int digits, int datacenterIdBits, int workerIdBits,
      int seqBits, boolean fixedDigitsEnabled, Class<? extends LocalGuid> guidClz) {
    return lenientFormat("%s-%s-%s-%s-%s-%s-%s-%s", datacenterId, workerId, digits, datacenterIdBits, workerIdBits,
        seqBits, fixedDigitsEnabled,
        Optional.ofNullable(RESERVED_KEY_SUFFIX.get(guidClz)).orElseGet(() -> guidClz.getName()));
  }

  // mainly for test purpose
  public static LocalGuid instance() {
    return instance(19, 5, 5, 12, false);
  }

}
