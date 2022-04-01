/*
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
package com.stun4j.guid;

import static com.stun4j.guid.utils.Asserts.argument;
import static com.stun4j.guid.utils.Asserts.notNull;
import static com.stun4j.guid.utils.Strings.lenientFormat;

import java.security.SecureRandom;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stun4j.guid.support.UUID;
import com.stun4j.guid.support.UUIDFast;
import com.stun4j.guid.utils.NetworkUtils;
import com.stun4j.guid.utils.Utils;
import com.stun4j.guid.utils.Utils.Pair;

/**
 * Guid generator,without any remote-coordination
 * <p>
 * {@link #next()} based on twitter-snowflake algorithm<br>
 * {@link #uuid()} based on jdk {@link java.util.UUID} (lower-case, without "-")
 * @author Jay Meng
 */
public final class LocalGuid {
  private static final Logger LOG = LoggerFactory.getLogger(LocalGuid.class);
  private static final long DC_ID_BITS_NUM = 5L;
  private static final long WK_ID_BITS_NUM = 5L;
  private static final long SEQ_BITS_NUM = 12L;
  private static final long MAX_DC_ID = ~(-1L << DC_ID_BITS_NUM);
  private static final long MAX_WK_ID = ~(-1L << WK_ID_BITS_NUM);
  private static final long WK_ID_SHIFT = SEQ_BITS_NUM;
  private static final long DC_ID_SHIFT = SEQ_BITS_NUM + WK_ID_BITS_NUM;
  private static final long TIMESTAMPE_SHIFT = SEQ_BITS_NUM + WK_ID_BITS_NUM + DC_ID_BITS_NUM;
  private static final long SEQ_MASK = ~(-1L << SEQ_BITS_NUM);

  // Fri Feb 14 16:12:19 CST 2020
  private final long epoch = 1581667939311L;
  private long datacenterId = -1L;
  private long workerId = -1L;
  private long sequence = 0L;
  private long lastTimestamp = -1L;

  private int sequenceOffset = -1;

  private static final AtomicReference<LocalGuid> INSTANCE = new AtomicReference<>();

  private static final UUIDFast UUID_FAST = new UUIDFast(new SecureRandom());

  public synchronized static LocalGuid init(int datacenterId, int workerId) {
    LocalGuid cur;
    if (INSTANCE.compareAndSet(null, cur = new LocalGuid())) {
      try {
        LocalGuid instance = cur.doCoreInit(datacenterId, workerId);
        LOG.info("The local-guid is successfully initialized [datacenterId={}, workerId={}]", datacenterId, workerId);
        return instance;
      } catch (Throwable e) {
        reset();
        throw e;
      }
    }
    cur = INSTANCE.get();
    long curDatacenterId;
    long curWorkerId;
    synchronized (cur) {
      curDatacenterId = cur.datacenterId;
      curWorkerId = cur.workerId;
    }
    if (curDatacenterId != datacenterId || curWorkerId != workerId) {
      LOG.warn("The local-guid has already been initialized,new initialization was ignored [current={}, new={}]",
          lenientFormat("dcId:%s,wkId:%s", curDatacenterId, curWorkerId),
          lenientFormat("dcId:%s,wkId:%s", datacenterId, workerId));
    }
    return cur;
  }

  public static LocalGuid initWithLocalIp() {
    return initWithLocalIp(null);
  }

  public synchronized static LocalGuid initWithLocalIp(String ipStartWith, int... theThirdSegmentRange) {
    String localIp = NetworkUtils.getLocalhost(ipStartWith, theThirdSegmentRange);
    int last3 = Integer.parseInt(localIp.substring(localIp.lastIndexOf(".") + 1));
    // TODO mj:If bit-editing is available, don't forget that this may need to change as well->
    int datacenterId = last3 >> 5;
    int workerId = (int)(last3 & ~(-1L << 5L));
    // <-
    LOG.info("The local-guid is initializing with [local-ip={}, datacenterId={}, workerId={}]", localIp, datacenterId,
        workerId);
    return init(datacenterId, workerId);
  }

  public static boolean isAllowInitialization() {
    return INSTANCE.get() == null;
  }

  public static LocalGuid instance() {
    LocalGuid instance;
    argument((instance = INSTANCE.get()) != null && instance.datacenterId >= 0 && instance.workerId >= 0,
        "The local-guid must be initialized in the very begining");
    return instance;
  }

  public static String uuid() {
    return uuid(false, true);
  }

  public static String uuid(boolean withHyphen) {
    return uuid(withHyphen, true);
  }

  public static String uuid(boolean withHyphen, boolean ultrafast) {
    return withHyphen ? (ultrafast ? UUID_FAST.generate().toString() : UUID.randomUUID().toString())
        : (ultrafast ? UUID_FAST.generate().toStringWithoutHyphen() : UUID.randomUUID().toStringWithoutHyphen());
  }

  public synchronized long next() {
    long timestamp = currentTimeMs();
    // handle time clock backwards(coz NTP is not safe)
    if (timestamp < this.lastTimestamp) {
      long timeLagMs = this.lastTimestamp - timestamp;
      if (timeLagMs >= 5000) {// TODO mj:5000->config
        String msg = lenientFormat("Clock moving backwards detected,too much time lag [lag=%sms]",
            lastTimestamp - timestamp);
        LOG.error(msg);
        throw new RuntimeException(msg);
      }
      LOG.warn("Clock moving backwards detected [time lag={}ms],try self-healing now...", timeLagMs);
      Pair<Long, Long> rtn = Utils.timeAwareRun(timeMsToChase -> {
        long curTimeMsChasing;
        // TODO mj:use random sleep time to reduce cpu cost
        while ((curTimeMsChasing = currentTimeMs()) < this.lastTimestamp) {
          // if (timeMsToChase-- > 0) {
          // Utils.sleepMs(1);
          // }
          if (timeMsToChase > 0) {
            int randomSleepMs = ThreadLocalRandom.current().nextInt(5) + 1;
            timeMsToChase = timeMsToChase - randomSleepMs;
            Utils.sleepMs(randomSleepMs);
          }
        }
        return curTimeMsChasing;
      }, timeLagMs);
      LOG.info("Self healed from clock moving backwards, cost {}ms", rtn.getLeft());
      // now we've got timestamp chased
      timestamp = rtn.getRight();
    }

    // core process
    if (lastTimestamp == timestamp) {
      sequence = (sequence + 1) & SEQ_MASK;
      if (sequence == 0) {
        timestamp = casGetNextMs(lastTimestamp);
      }
    } else {
      // sequence = 0L;

      // a simple vibrate mechanism of guid generation,to improve the evenness of the distribution of odd and even
      // numbers,under certain circumstances, i.e. db-sharding key,
      // for more detail,also please check 'https://github.com/apache/shardingsphere /issues/1617'
      vibrateSequenceOffset();
      sequence = sequenceOffset;
    }

    lastTimestamp = timestamp;

    return from(timestamp);
  }

  // convenience method->
  public long from(Date dateTime) {
    notNull(dateTime, "The dateTime can't be null");
    return from(dateTime.getTime());
  }

  public synchronized long from(long timeMs) {
    return ((timeMs - epoch) << TIMESTAMPE_SHIFT) //
        | (datacenterId << DC_ID_SHIFT) //
        | (workerId << WK_ID_SHIFT) //
        | sequence;
  }

  public long getTimeMsFromId(long idExpectingSameEpoch) {
    return (idExpectingSameEpoch >> TIMESTAMPE_SHIFT & ~(-1L << 41L)) + this.epoch;
  }
  // <-

  private long casGetNextMs(long lastTimestamp) {
    long timestamp = currentTimeMs();
    while (timestamp <= lastTimestamp) {
      timestamp = currentTimeMs();
    }
    return timestamp;
  }

  private long currentTimeMs() {
    return System.currentTimeMillis();
  }

  private LocalGuid doCoreInit(int datacenterId, int workerId) {
    // basic check
    argument(datacenterId <= MAX_DC_ID && datacenterId >= 0, "The datacenterId can't be greater than %s or less than 0",
        MAX_DC_ID);
    argument(workerId <= MAX_WK_ID && workerId >= 0, "The workerId can't be greater than %s or less than 0", MAX_WK_ID);

    // check passed, do initialize
    synchronized (this) {
      this.datacenterId = datacenterId;
      this.workerId = workerId;
    }
    return this;
  }

  private void vibrateSequenceOffset() {
    sequenceOffset = sequenceOffset >= getMaxVibrationOffset() ? 0 : sequenceOffset + 1;
  }

  private int getMaxVibrationOffset() {
    // int result = Integer
    // .parseInt(properties.getProperty("max.vibration.offset", String.valueOf(DEFAULT_VIBRATION_VALUE)));
    // argument(result >= 0 && result <= SEQUENCE_MASK, "Illegal max vibration offset");
    // return result;//TODO mj:max.vibration->config
    return 1;
  }

  private LocalGuid() {
  }

  public long getDatacenterId() {
    return datacenterId;
  }

  public long getWorkerId() {
    return workerId;
  }

  static LocalGuid init(Pair<Integer, Integer> nodeInfo) {
    return init(nodeInfo.getLeft(), nodeInfo.getRight());
  }

  // mainly for reconnect purpose
  synchronized void reset(Pair<Integer, Integer> newNodeInfo) {
    // compare and check
    long curDatacenterId = this.datacenterId;
    long curWorkerId = this.workerId;
    int newDatacenterId = newNodeInfo.getLeft();
    int newWorkerId = newNodeInfo.getRight();
    if (curDatacenterId == newDatacenterId && curWorkerId == newWorkerId) {
      LOG.warn(
          "Neither the datacenterId nor the workerId changed,the local-guid reset was ignored [current={}, new={}]",
          lenientFormat("dcId:%s,wkId:%s", curDatacenterId, curWorkerId),
          lenientFormat("dcId:%s,wkId:%s", newDatacenterId, newWorkerId));
      return;
    }

    // do reset
    LOG.warn("The datacenterId or workerId is being changed [current={}, new={}]",
        lenientFormat("dcId:%s,wkId:%s", curDatacenterId, curWorkerId),
        lenientFormat("dcId:%s,wkId:%s", newDatacenterId, newWorkerId));
    doCoreInit(newDatacenterId, newWorkerId);
    LOG.info("The local-guid is successfully reset [datacenterId={}, workerId={}]", newDatacenterId, newWorkerId);
  }

  // mainly for test purpose(extremely rare for init-rollback)
  // TODO mj:building a cross-classloader test instead
  synchronized static void reset() {
    LocalGuid instance = INSTANCE.get();
    if (instance == null) {
      return;
    }
    synchronized (instance) {
      instance.datacenterId = -1L;
      instance.workerId = -1L;
      instance.sequence = 0L;
      instance.lastTimestamp = -1L;
    }
    INSTANCE.set(null);
  }

}