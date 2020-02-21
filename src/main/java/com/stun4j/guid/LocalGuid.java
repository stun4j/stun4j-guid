package com.stun4j.guid;

import java.util.UUID;

/**
 * Guid generator,without any remote-coordination
 * <p>
 * {@link #next()} based on twitter-snowflake algorithm<br>
 * {@link #uuid()} based on jdk {@link java.util.UUID} (without "-", lower-case)
 * 
 * @author Jay Meng
 */
public final class LocalGuid {
  // TODO mj:cross classloader unique?
  // Fri Feb 14 16:12:19 CST 2020 1581667939311
  private final long epoch = 1581667939311L;
  private final long workerIdBits = 5L;
  private final long datacenterIdBits = 5L;

  private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
  private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
  private final long sequenceBits = 12L;
  private final long workerIdShift = sequenceBits;
  private final long datacenterIdShift = sequenceBits + workerIdBits;
  private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
  private final long sequenceMask = -1L ^ (-1L << sequenceBits);
  private long datacenterId = -1;
  private long workerId = -1;
  private long sequence = 0L;
  private long lastTimestamp = -1L;

  private static final LocalGuid INSTANCE = new LocalGuid();
  private static boolean initialized = false;

  public synchronized static LocalGuid init(long datacenterId, long workerId) {
    if (initialized) {
      return INSTANCE;
    }
    INSTANCE.datacenterId = datacenterId;
    INSTANCE.workerId = workerId;
    initialized = true;
    return INSTANCE;
  }

  public static LocalGuid instance() {
    if (!initialized) {
      throw new IllegalArgumentException("must be initialized first");
    }
    // TODO mj:works?dirty?compare to volatile?
    if (INSTANCE.datacenterId <= -1 || INSTANCE.workerId <= -1) {
      throw new IllegalArgumentException("being initialized");
    }
    return INSTANCE;
  }

  private LocalGuid(long datacenterId, long workerId) {
    if (datacenterId > maxDatacenterId || datacenterId < 0) {
      throw new IllegalArgumentException(
          String.format("datacenter id can't be greater than %d or less than 0", maxDatacenterId));
    }
    if (workerId > maxWorkerId || workerId < 0) {
      throw new IllegalArgumentException(
          String.format("worker id can't be greater than %d or less than 0", maxWorkerId));
    }
    this.datacenterId = datacenterId;
    this.workerId = workerId;
  }

  public static String uuid() {
    return UUID.randomUUID().toString().replaceAll("-", "").toLowerCase();
  }

  public synchronized long next() {
    long timestamp = currentTimeMillis();

    // FIXME mj:time clock unexpected back,even under NTP env
    if (timestamp < lastTimestamp) {
      throw new RuntimeException(String.format("时钟倒退了 %dms,拒绝产生新的id", lastTimestamp - timestamp));
    }

    if (lastTimestamp == timestamp) {
      sequence = (sequence + 1) & sequenceMask;
      if (sequence == 0) {
        timestamp = casGetNextMillis(lastTimestamp);
      }
    } else {
      sequence = 0L;
    }

    lastTimestamp = timestamp;

    return ((timestamp - epoch) << timestampLeftShift) //
        | (datacenterId << datacenterIdShift) //
        | (workerId << workerIdShift) //
        | sequence;
  }

  private long casGetNextMillis(long lastTimestamp) {
    long timestamp = currentTimeMillis();
    while (timestamp <= lastTimestamp) {
      timestamp = currentTimeMillis();
    }
    return timestamp;
  }

  private long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  private LocalGuid() {
  }
}