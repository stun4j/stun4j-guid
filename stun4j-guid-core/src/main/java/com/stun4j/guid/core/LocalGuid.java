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

  public static boolean _show_initialization_report = true;
  public static boolean _max_node_limited = true;
  public static int _limited_max_node = 1024;

  private static final AtomicReference<LocalGuid> INSTANCE = new AtomicReference<>();
  private static final UUIDFast UUID_FAST = new UUIDFast(new SecureRandom());

  private final long datacenterIdBits;
  private final long workerIdBits;
  private final long seqBits;
  private final long workerIdShift;
  private final long datacenterIdShift;
  private final long timestampShift;
  private final long seqMask;
  private final long maxDeltaMs;
  private final long minDeltaMs;
  private final int maxNode;

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

  public static LocalGuid initWithLocalIp(int digits, boolean shortDcWkIdBitsEnabled, int seqBits,
      boolean fixedDigitsEnabled) {
    return initWithLocalIp(digits, shortDcWkIdBitsEnabled, seqBits, fixedDigitsEnabled, null);
  }

  public synchronized static LocalGuid initWithLocalIp(int digits, boolean shortDcWkIdBitsEnabled, int seqBits,
      boolean fixedDigitsEnabled, String ipStartWith, int... theThirdSegmentRange) {
    String localIp = NetworkUtils.getLocalhost(ipStartWith, theThirdSegmentRange);
    int last3 = Integer.parseInt(localIp.substring(localIp.lastIndexOf(".") + 1));
    int bits = shortDcWkIdBitsEnabled ? 4 : 5;
    int datacenterId = last3 >> bits;
    int workerId = last3 & ~(-1 << bits);
    LOG.info("The local-guid is initializing with [local-ip={}, datacenterId={}, workerId={}]", localIp, datacenterId,
        workerId);
    return init(datacenterId, workerId, digits, bits, bits, seqBits, fixedDigitsEnabled);
  }

  public synchronized static LocalGuid init(int datacenterId, int workerId, int digits, int datacenterIdBits,
      int workerIdBits, int seqBits, boolean fixedDigitsEnabled) {
    return init(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled,
        LocalGuid.class);
  }

  public static boolean isAllowInitialization() {
    return INSTANCE.get() == null;
  }

  public static LocalGuid instance() {
    LocalGuid instance;
    argument((instance = INSTANCE.get()) != null, "The local-guid must be initialized in the very begining");
    synchronized (instance) {
      return instance;
    }
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
    // Always start with our own epoch,even if reset after the max-date has been exceeded
    return (idExpectingSameEpoch >> timestampShift & ~(-1L << 41L)) + this.epoch;
  }

  protected long currentTimeMs() {
    return System.currentTimeMillis();
  }

  final void doInitDcWkId(int datacenterId, int workerId) {
    long maxDcId = ~(-1L << datacenterIdBits);
    long maxWkId = ~(-1L << workerIdBits);
    // basic check
    argument(datacenterId <= maxDcId && datacenterId >= 0, "The datacenterId can't be greater than %s or less than 0",
        maxDcId);
    argument(workerId <= maxWkId && workerId >= 0, "The workerId can't be greater than %s or less than 0", maxWkId);

    // check passed, do initialize
    synchronized (this) {
      this.datacenterId = datacenterId;
      this.workerId = workerId;
    }
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

  synchronized static LocalGuid init(int datacenterId, int workerId, int digits, int datacenterIdBits, int workerIdBits,
      int seqBits, boolean fixedDigitsEnabled, Class<? extends LocalGuid> guidClz) {
    LocalGuid instance = null;
    try {
      if (INSTANCE
          .compareAndSet(null,
              instance = guidClz.getDeclaredConstructor(int.class, int.class, int.class, int.class, int.class,
                  int.class, boolean.class).newInstance(datacenterId, workerId, digits, datacenterIdBits, workerIdBits,
                      seqBits, fixedDigitsEnabled))) {
        doMultiInstanceAwareInit(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits,
            fixedDigitsEnabled, guidClz, instance);
        logSuccessfullyInit(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled,
            LOG);
        return instance;
      }
    } catch (Throwable t) {
      reset();
      Exceptions.sneakyThrow(t);
    }
    // multi-instance register if necessary->
    doMultiInstanceAwareInit(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits,
        fixedDigitsEnabled, guidClz, instance);
    // <-

    instance = INSTANCE.get();
    long curDatacenterId;
    long curWorkerId;
    synchronized (instance) {
      curDatacenterId = instance.datacenterId;
      curWorkerId = instance.workerId;
    }
    if (curDatacenterId != datacenterId || curWorkerId != workerId) {
      LOG.warn("The local-guid has already been initialized, new initialization is ignored! [current={}, new={}]",
          lenientFormat("dcId:%s,wkId:%s", curDatacenterId, curWorkerId),
          lenientFormat("dcId:%s,wkId:%s", datacenterId, workerId));
    }
    return instance;
  }

  private static void doMultiInstanceAwareInit(int datacenterId, int workerId, int digits, int datacenterIdBits,
      int workerIdBits, int seqBits, boolean fixedDigitsEnabled, Class<? extends LocalGuid> guidClz,
      LocalGuid instance) {
    if (LocalGuidMultiton._enabled) {
      LocalGuidMultiton.putIfAbsent(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits,
          fixedDigitsEnabled, guidClz, instance);// make multi-instance registry work
    }
  }

  static void logSuccessfullyInit(int datacenterId, int workerId, int digits, int datacenterIdBits, int workerIdBits,
      int seqBits, boolean fixedDigitsEnabled, Logger log) {
    log.info(
        "The local-guid is successfully initialized [dcId={}, wkId={}, digits={}, dcIdBits={}, wkIdBits={}, seqBits={}, fixedDigits={}]",
        datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled);
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
    doInitDcWkId(newDatacenterId, newWorkerId);
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

  LocalGuid(int datacenterId, int workerId, int digits, int datacenterIdBits, int workerIdBits, int seqBits,
      boolean fixedDigitsEnabled) {
    this(datacenterId, workerId, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled, true,
        _show_initialization_report);
  }

  LocalGuid(int datacenterId, int workerId, int digits, int datacenterIdBits, int workerIdBits, int seqBits,
      boolean fixedDigitsEnabled, boolean initDcWk, boolean showReport) {
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
    argument(datacenterIdBits > 0, "The local-guid datacenterIdBits must be greater than 0");
    argument(workerIdBits > 0, "The local-guid workerIdBits must be greater than 0");
    argument(seqBits > 0, "The local-guid seqBits must be greater than 0");

    this.datacenterIdBits = datacenterIdBits;
    this.workerIdBits = workerIdBits;
    this.seqBits = seqBits;
    this.workerIdShift = seqBits;
    this.datacenterIdShift = seqBits + workerIdBits;
    this.seqMask = ~(-1L << seqBits);
    this.fixedDigitsEnabled = fixedDigitsEnabled;
    long timestampShift = this.timestampShift = seqBits + workerIdBits + datacenterIdBits;
    this.maxDeltaMs = idMaxVal >> timestampShift;
    this.minDeltaMs = idMinVal >> timestampShift;

    Date maxDate;
    Date nowDate;
    int maxNode = this.maxNode = 1 << (datacenterIdBits + workerIdBits);
    argument(
        (maxDate = new Date(epoch + (!fixedDigitsEnabled ? maxDeltaMs : maxDeltaMs - minDeltaMs)))
            .compareTo(nowDate = new Date()) > 0,
        "A good id time-factor date should be much later than current date [max-date=%s, current-date=%s]", maxDate,
        nowDate);
    long delta = maxDate.getTime() - nowDate.getTime();
    argument(delta >= Utils.yearsLongMillis(5),
        "A good id time-factor date should be at least 5 years later than current date [max-date=%s, current-date=%s]",
        maxDate, nowDate);
    if (_max_node_limited) {
      argument(maxNode <= _limited_max_node,
          "The max-node can't be greater than %s > The sum of DC-bit and WK-bit cannot exceed %s", _limited_max_node,
          Integer.toBinaryString(_limited_max_node).length() - 1);
    }

    if (initDcWk) {
      doInitDcWkId(datacenterId, workerId);

      if (fixedDigitsEnabled) {
        long idTry = this.next();
        Consumer<Integer> assertDigits = actualDigits -> {
          argument(digits == actualDigits, "The local-guid digits not matched [expected digits=%s, actual id=%s]",
              digits, NumberFormat.getInstance().format(idTry));
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
        } else {
          throw new IllegalArgumentException("The local-guid digits range can only be [15,19], but the actual id was "
              + NumberFormat.getInstance().format(idTry));
        }
      }
    }

    // the report
    if (!showReport) return;
    LOG.info("--- Stun4J Guid initialization additional information ---");
    LOG.info("Theoretical tps: {}", (1 << seqBits) * 1000);
    LOG.info("Theoretical max-date: {}", !fixedDigitsEnabled ? maxDate : new Date(epoch + maxDeltaMs));
    LOG.info("     Actual max-date: {}", maxDate);
    if (fixedDigitsEnabled) {
      LOG.info("Theoretical min-date: {}", new Date(epoch + minDeltaMs));
    }
    LOG.info("Theoretical max-node: {}", maxNode);
    LOG.info("---------------------------------------------------------");
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

  public final long getDatacenterIdBits() {
    return datacenterIdBits;
  }

  public final long getWorkerIdBits() {
    return workerIdBits;
  }

  public final long getSeqBits() {
    return seqBits;
  }

  public final int getMaxNode() {
    return maxNode;
  }

  final long getMaxDeltaMs() {
    return maxDeltaMs;
  }

}