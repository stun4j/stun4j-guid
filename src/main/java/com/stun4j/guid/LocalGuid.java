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
package com.stun4j.guid;

import static com.stun4j.guid.utils.Asserts.argument;
import static com.stun4j.guid.utils.Asserts.notNull;
import static com.stun4j.guid.utils.Asserts.state;
import static com.stun4j.guid.utils.Strings.leftPad;
import static com.stun4j.guid.utils.Strings.lenientFormat;

import java.security.SecureRandom;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stun4j.guid.support.UUID;
import com.stun4j.guid.support.UUIDFast;
import com.stun4j.guid.utils.Pair;
import com.stun4j.guid.utils.Utils;

/**
 * Guid generator,without any remote-coordination
 * <p>
 * {@link #next()} based on twitter-snowflake algorithm<br>
 * {@link #uuid()} based on jdk {@link java.util.UUID} (lower-case, without "-")
 * 
 * @author Jay Meng
 */
public final class LocalGuid {
  private static final Logger LOG = LoggerFactory.getLogger(LocalGuid.class);
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
  private long datacenterId = -1L;
  private long workerId = -1L;
  private long sequence = 0L;
  private long lastTimestamp = -1L;

  private int sequenceOffset = -1;

  private static final LocalGuid INSTANCE = new LocalGuid();
  private static boolean initialized = false;
  private static boolean gate = false;

  private final UUIDFast uuidFast = new UUIDFast(new SecureRandom());

  public static LocalGuid init(int datacenterId, int workerId) {
    return init(Pair.of(datacenterId, workerId));
  }

  public synchronized static LocalGuid init(Pair<Integer, Integer> node) {
    if (initialized) {
      LOG.warn("no need to initialize it again&again, the incoming node-info won't take effect [current={}, new={}]",
          Pair.of(INSTANCE.datacenterId, INSTANCE.workerId), node);
      return INSTANCE;
    }

    INSTANCE.doCoreInit(node);
    initialized = true;
    LOG.info("Local guid successfully initialized [datacenterId={}, workerId={}]", node.getLeft(), node.getRight());
    return INSTANCE;
  }

  public static LocalGuid instance() {
    argument(initialized, "Local guid must be initialized in the very begining");
    // TODO mj:instead of 'synchronized/volatile/happens-before/out-of-order' stuffs,hope this way works
    // TODO mj:provider patterns for user to choose,in reset/reconnect scenario
    state(gate || (INSTANCE.datacenterId > -1 && INSTANCE.workerId > -1 && (gate = true)), "being initialized");
    return INSTANCE;
  }

  public static String uuid() {
    return uuid(false, true);
  }

  public static String uuid(boolean withHyphen) {
    return uuid(withHyphen, true);
  }

  public static String uuid(boolean withHyphen, boolean ultrafast) {
    return withHyphen ? (ultrafast ? INSTANCE.uuidFast.generate().toString() : UUID.randomUUID().toString())
        : (ultrafast ? INSTANCE.uuidFast.generate().toStringWithoutHyphen()
            : UUID.randomUUID().toStringWithoutHyphen());
  }

  public synchronized long next() {
    long timestamp = currentTimeMs();

    // handle time clock backwards(coz NTP is not safe)
    if (timestamp < this.lastTimestamp) {
      long timeLagMs = this.lastTimestamp - timestamp;
      if (timeLagMs >= 5000) {
        String msg = lenientFormat("clock moving backwards detected,too much time lag [lag=%sms]",
            lastTimestamp - timestamp);
        LOG.error(msg);
        throw new RuntimeException(msg);
      }
      LOG.warn("clock moving backwards detected [time lag={}ms],try self-healing now...", timeLagMs);
      Pair<Long, Long> rtn = Utils.timeAwareRun((timeMsToChase) -> {
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
      LOG.info("self-healed from clock moving backwards, cost {}ms", rtn.getLeft());
      // now we've got timestamp chased
      timestamp = rtn.getRight();
    }

    // core process
    if (lastTimestamp == timestamp) {
      sequence = (sequence + 1) & sequenceMask;
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
    notNull(dateTime, "datetime can't be null");
    return from(dateTime.getTime());
  }

  public long from(long timeMs) {
    return ((timeMs - epoch) << timestampLeftShift) //
        | (datacenterId << datacenterIdShift) //
        | (workerId << workerIdShift) //
        | sequence;
  }

  public long getTimeMsFromId(long idExpectingSameEpoch) {
    // assume the id is an unsigned num
    String binStr = Long.toUnsignedString(idExpectingSameEpoch, 2);
    binStr = leftPad(binStr, 64, "0");
    String timeDeltaStr = binStr.substring(1, 42);
    long back = Long.valueOf(timeDeltaStr, 2);
    return back + this.epoch;
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

  private void doCoreInit(Pair<Integer, Integer> node) {
    // basic check
    long datacenterId = node.getLeft();
    long workerId = node.getRight();
    long maxDatacenterId = this.maxDatacenterId;
    long maxWorkerId = this.maxWorkerId;
    argument(datacenterId <= maxDatacenterId && datacenterId >= 0,
        "datacenterId can't be greater than %s or less than 0", maxDatacenterId);
    argument(workerId <= maxWorkerId && workerId >= 0, "workerId can't be greater than %s or less than 0", maxWorkerId);

    // check passed, do initialize
    this.datacenterId = datacenterId;
    this.workerId = workerId;
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

  // mainly for reconnect purpose
  // ----------------------------------------------------------------
  synchronized void reset(Pair<Integer, Integer> newNode) {
    // compare and check
    long curDatacenterIdId = INSTANCE.datacenterId;
    long curWorkerId = INSTANCE.workerId;
    long newDatacenterId = newNode.getLeft();
    long newWorkerId = newNode.getRight();
    if (curDatacenterIdId == newDatacenterId && curWorkerId == newWorkerId) {
      LOG.info("neither the datacenterId nor the workerId has changed,Local guid reset ignored [current={}, new={}]",
          lenientFormat("dcId:%s,wkId:%s", curDatacenterIdId, curWorkerId),
          lenientFormat("dcId:%s,wkId:%s", newDatacenterId, newWorkerId));
      return;
    }

    // do reset
    LOG.warn("datacenterId or workerId being changed [current={}, new={}]",
        lenientFormat("dcId:%s,wkId:%s", curDatacenterIdId, curWorkerId),
        lenientFormat("dcId:%s,wkId:%s", newDatacenterId, newWorkerId));
    doCoreInit(newNode);
    // FIXME mj:wot if ex here?
    LOG.info("Local guid successfully reset [datacenterId={}, workerId={}]", newDatacenterId, newWorkerId);
  }

  // only for test purpose
  // FIXME mj:building a cross-classload test instead
  // ----------------------------------------------------------------
  void reset() {
    datacenterId = -1L;
    workerId = -1L;
    sequence = 0L;
    lastTimestamp = -1L;
    initialized = false;
    gate = false;
  }

}