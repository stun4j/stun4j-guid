package com.stun4j.guid.core;

import static com.stun4j.guid.core.utils.Strings.lenientFormat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalGuidMulti {
  public static volatile boolean _enabled = false;
  private static final Map<String, LocalGuid> REG = new ConcurrentHashMap<>();

  static LocalGuid putIfAbsent(int datacenterId, int workerId, int digits, long datacenterIdBits, long workerIdBits,
      long seqBits, boolean fixedDigitsEnabled, Class<? extends LocalGuid> guidClz, LocalGuid instance) {
    String key = keyBy(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled,
        guidClz);
    LocalGuid old = REG.putIfAbsent(key, instance);
    return old;
  }

  public static LocalGuid instance(int datacenterId, int workerId) {
    return instance(datacenterId, workerId, 19, 5, 5, 12, false);
  }

  public static LocalGuid instance(int datacenterId, int workerId, int digits, long datacenterIdBits, long workerIdBits,
      long seqBits, boolean fixedDigitsEnabled) {
    return instance(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled,
        LocalGuid.class);
  }

  public static LocalGuid instance(int datacenterId, int workerId, int digits, long datacenterIdBits, long workerIdBits,
      long seqBits, boolean fixedDigitsEnabled, Class<? extends LocalGuid> guidClz) {
    String key = keyBy(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled,
        guidClz);
    return REG.get(key);
  }

  private static String keyBy(int datacenterId, int workerId, int digits, long datacenterIdBits, long workerIdBits,
      long seqBits, boolean fixedDigitsEnabled, Class<? extends LocalGuid> guidClz) {
    return lenientFormat("%s-%s-%s-%s-%s-%s-%s-%s", datacenterId, workerId, digits, datacenterIdBits, workerIdBits,
        seqBits, fixedDigitsEnabled, guidClz.getName());
  }

}
