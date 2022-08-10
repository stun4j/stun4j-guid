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

import static com.stun4j.guid.core.utils.Asserts.argument;
import static com.stun4j.guid.core.utils.Asserts.notNull;
import static com.stun4j.guid.core.utils.Asserts.state;
import static com.stun4j.guid.core.utils.Strings.lenientFormat;

import java.security.SecureRandom;
import java.text.NumberFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stun4j.guid.core.support.UUID;
import com.stun4j.guid.core.support.UUIDFast;
import com.stun4j.guid.core.utils.Exceptions;
import com.stun4j.guid.core.utils.NetworkUtils;
import com.stun4j.guid.core.utils.Utils;
import com.stun4j.guid.core.utils.Utils.Pair;

/**
 * Guid generator,without any remote-coordination
 * <p>
 * {@link #next()} based on twitter-snowflake algorithm<br>
 * {@link #uuid()} based on jdk {@link java.util.UUID} (lower-case, without "-")
 * @author Jay Meng
 */
public class LocalGuid {
  private static final Logger LOG = LoggerFactory.getLogger(LocalGuid.class);

  private static final AtomicReference<LocalGuid> INSTANCE = new AtomicReference<>();
  private static final UUIDFast UUID_FAST = new UUIDFast(new SecureRandom());

  private final long datacenterIdBitsNum;
  private final long workerIdBitsNum;
  private final long seqBitsNum;
  private final long workerIdShift;
  private final long datacenterIdShift;
  private final long timestampShift;
  private final long seqMask;
  private final long maxDeltaMs;
  private final long minDeltaMs;

  // Fri Feb 14 16:12:19 CST 2020
  private final long epoch = 1581667939311L;
  private long datacenterId = -1L;
  private long workerId = -1L;
  private long sequence = 0L;
  private long lastTimestamp = -1L;

  private int sequenceOffset = -1;

  private boolean fixedDigitsEnabled = false;

  public static LocalGuid init(int datacenterId, int workerId) {
    return init(datacenterId, workerId, 19, 5, 5, 12, false);
  }

  public static LocalGuid initWithLocalIp() {
    return initWithLocalIp(null);
  }

  public static LocalGuid initWithLocalIp(String ipStartWith) {
    return initWithLocalIp(19, false, 12, false, ipStartWith);
  }

  public static LocalGuid initWithLocalIp(int digits, boolean shortDcWkIdBitsEnabled, long seqBitsNum,
      boolean fixedDigitsEnabled) {
    return initWithLocalIp(digits, shortDcWkIdBitsEnabled, seqBitsNum, fixedDigitsEnabled, null);
  }

  public synchronized static LocalGuid initWithLocalIp(int digits, boolean shortDcWkIdBitsEnabled, long seqBitsNum,
      boolean fixedDigitsEnabled, String ipStartWith, int... theThirdSegmentRange) {
    String localIp = NetworkUtils.getLocalhost(ipStartWith, theThirdSegmentRange);
    int last3 = Integer.parseInt(localIp.substring(localIp.lastIndexOf(".") + 1));
    long bitsNum = shortDcWkIdBitsEnabled ? 4L : 5L;
    int datacenterId = last3 >> (int)(bitsNum);
    int workerId = (int)(last3 & ~(-1L << bitsNum));
    LOG.info("The local-guid is initializing with [local-ip={}, datacenterId={}, workerId={}]", localIp, datacenterId,
        workerId);
    return init(datacenterId, workerId, digits, bitsNum, bitsNum, seqBitsNum, fixedDigitsEnabled);
  }

  public synchronized static LocalGuid init(int datacenterId, int workerId, int digits, long datacenterIdBitsNum,
      long workerIdBitsNum, long seqBitsNum, boolean fixedDigitsEnabled) {
    return init(datacenterId, workerId, digits, datacenterIdBitsNum, workerIdBitsNum, seqBitsNum, fixedDigitsEnabled,
        LocalGuid.class);
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

  public final synchronized long next() {
    long timestamp = currentTimeMs();
    // handle retrograde clock change(coz NTP is not safe)
    if (timestamp < this.lastTimestamp) {
      long timeLagMs = this.lastTimestamp - timestamp;
      if (timeLagMs >= 5000) {// TODO mj:5000->config
        String msg = lenientFormat("Retrograde clock change detected,too much time lag [lag=%sms]",
            lastTimestamp - timestamp);
        LOG.error(msg);
        throw new RuntimeException(msg);
      }
      LOG.warn("Retrograde clock change detected [time lag={}ms],try self-healing now...", timeLagMs);
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
      LOG.info("Self healed from retrograde clock changing, cost {}ms", rtn.getLeft());
      // now we've got timestamp chased
      timestamp = rtn.getRight();
    }

    // core process
    if (lastTimestamp == timestamp) {
      sequence = (sequence + 1) & seqMask;
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
  public final long from(Date dateTime) {
    notNull(dateTime, "The dateTime can't be null");
    return from(dateTime.getTime());
  }

  public final synchronized long from(long timeMs) {
    long deltaMs = timeMs - epoch;
    if (deltaMs > maxDeltaMs) {
      if (fixedDigitsEnabled) {
        deltaMs = deltaMs % (maxDeltaMs - minDeltaMs) + minDeltaMs;
      } else {
        deltaMs %= maxDeltaMs;
      }
    }
    return (deltaMs << timestampShift) //
        | (datacenterId << datacenterIdShift) //
        | (workerId << workerIdShift) //
        | sequence;
  }

  public final long getTimeMsFromId(long idExpectingSameEpoch) {
    // TODO mj:If bit-editing is available, don't forget that this may need to change as wel
    return (idExpectingSameEpoch >> timestampShift & ~(-1L << 41L)) + this.epoch;
  }
  // <-

  protected long currentTimeMs() {
    return System.currentTimeMillis();
  }

  final LocalGuid doCoreInit(int datacenterId, int workerId) {
    long maxDcId = ~(-1L << datacenterIdBitsNum);
    long maxWkId = ~(-1L << workerIdBitsNum);
    // basic check
    argument(datacenterId <= maxDcId && datacenterId >= 0, "The datacenterId can't be greater than %s or less than 0",
        maxDcId);
    argument(workerId <= maxWkId && workerId >= 0, "The workerId can't be greater than %s or less than 0", maxWkId);

    // check passed, do initialize
    synchronized (this) {
      this.datacenterId = datacenterId;
      this.workerId = workerId;
    }
    return this;
  }

  private long casGetNextMs(long lastTimestamp) {
    long timestamp = currentTimeMs();
    while (timestamp <= lastTimestamp) {
      timestamp = currentTimeMs();
    }
    return timestamp;
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

  static LocalGuid init(Pair<Integer, Integer> nodeInfo) {
    return init(nodeInfo.getLeft(), nodeInfo.getRight());
  }

  synchronized static LocalGuid init(int datacenterId, int workerId, int digits, long datacenterIdBitsNum,
      long workerIdBitsNum, long seqBitsNum, boolean fixedDigitsEnabled, Class<? extends LocalGuid> guidClz) {
    LocalGuid cur;
    try {
      if (INSTANCE.compareAndSet(null,
          cur = guidClz.getDeclaredConstructor(int.class, long.class, long.class, long.class, boolean.class)
              .newInstance(digits, datacenterIdBitsNum, workerIdBitsNum, seqBitsNum, fixedDigitsEnabled))) {
        LocalGuid instance = cur.doCoreInit(datacenterId, workerId);
        LOG.info(
            "The local-guid is successfully initialized [dcId={}, wkId={}, digits={}, dcIdBitsNum={}, wkIdBitsNum={}, seqBitsNum={}, fixedDigits={}]",
            datacenterId, workerId, digits, datacenterIdBitsNum, workerIdBitsNum, seqBitsNum, fixedDigitsEnabled);

        // An immediate fixed-digits test(this is a post-assertion and may not be necessary)
        if (fixedDigitsEnabled) {
          long idTry = instance.next();
          Consumer<Integer> assertDigits = expectedDigits -> {
            state(digits == expectedDigits, "The local-guid digits not matched [expected digits=%s, actual id=%s]", expectedDigits,
                NumberFormat.getInstance().format(idTry));
          };
          if (idTry >= 10000_00000_00000L && idTry <= 99999_99999_99999L) {
            assertDigits.accept(15);
          } else if (idTry >= 10000_00000_00000_0L && idTry <= 99999_99999_99999_9L) {
            assertDigits.accept(16);
          } else if (idTry >= 10000_00000_00000_00L && idTry <= 99999_99999_99999_99L) {
            assertDigits.accept(17);
          } else if (idTry >= 10000_00000_00000_000L && idTry <= 99999_99999_99999_999L) {
            assertDigits.accept(18);
          } else if (idTry >= 10000_00000_00000_0000L && idTry <= Long.MAX_VALUE) {
            assertDigits.accept(19);
          }
        }
        return instance;
      }
    } catch (Throwable t) {
      reset();
      Exceptions.sneakyThrow(t);
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

  // mainly for reconnect purpose
  final synchronized void reset(Pair<Integer, Integer> newNodeInfo) {
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
      instance.fixedDigitsEnabled = false;
    }
    INSTANCE.set(null);
  }

  LocalGuid() {
    this(19, 5, 5, 12, false);
  }

  LocalGuid(int digits, long datacenterIdBitsNum, long workerIdBitsNum, long seqBitsNum, boolean fixedDigitsEnabled) {
    this.datacenterIdBitsNum = datacenterIdBitsNum;
    this.workerIdBitsNum = workerIdBitsNum;
    this.seqBitsNum = seqBitsNum;
    this.workerIdShift = seqBitsNum;
    this.datacenterIdShift = seqBitsNum + workerIdBitsNum;
    this.seqMask = ~(-1L << seqBitsNum);
    this.fixedDigitsEnabled = fixedDigitsEnabled;
    long idMaxVal;
    long idMinVal;
    switch (digits) {
      case 15:
        idMaxVal = 99999_99999_99999L;
        idMinVal = 10000_00000_00000L;
        break;
      case 16:
        idMaxVal = 99999_99999_99999_9L;
        idMinVal = 10000_00000_00000_0L;
        break;
      case 17:
        idMaxVal = 99999_99999_99999_99L;
        idMinVal = 10000_00000_00000_00L;
        break;
      case 18:
        idMaxVal = 99999_99999_99999_999L;
        idMinVal = 10000_00000_00000_000L;
        break;
      case 19:
        idMaxVal = Long.MAX_VALUE;
        idMinVal = 10000_00000_00000_0000L;
        break;
      default:
        throw new IllegalArgumentException("The local-guid digits range can only be [15,19]");
    }
    long timestampShift = this.timestampShift = seqBitsNum + workerIdBitsNum + datacenterIdBitsNum;
    this.maxDeltaMs = idMaxVal >> timestampShift;
    this.minDeltaMs = idMinVal >> timestampShift;

    Date maxDate;
    Date nowDate;
    state((maxDate = new Date(epoch + maxDeltaMs)).compareTo(nowDate = new Date()) > 0,
        "A good id time-factor date should be much later than current date [max-date=%s, current-date=%s]", maxDate,
        nowDate);
    long delta = maxDate.getTime() - nowDate.getTime();
    state(delta >= Utils.yearsLongMillis(5),
        "A good id time-factor date should be at least 5 years later than current date [max-date=%s, current-date=%s]",
        maxDate, nowDate);
    LOG.info(
        "\n--- Guid initialization additional information ---\nTheoretical tps: {}\nTheoretical max-date: {}\nTheoretical min-date: {}\n--------------------------------------------------\n",
        (1 << seqBitsNum) * 1000, maxDate, new Date(epoch + minDeltaMs));
  }

  public final long getEpoch() {
    return epoch;
  }

  public final long getDatacenterId() {
    return datacenterId;
  }

  public final long getWorkerId() {
    return workerId;
  }

  public final long getSeqBitsNum() {
    return seqBitsNum;
  }

  final long getMaxDeltaMs() {
    return maxDeltaMs;
  }

}