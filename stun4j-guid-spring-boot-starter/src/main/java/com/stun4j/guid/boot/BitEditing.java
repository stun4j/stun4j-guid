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
package com.stun4j.guid.boot;

import static com.stun4j.guid.core.utils.Asserts.state;

/**
 * Bit editing configuration
 * <p>
 * @author Jay Meng
 */
public class BitEditing {
  private static final String MSG = "The 'bit-editing' is not enabled";
  /**
   * Is bit editing function enabled
   * <p>
   * Default: false
   */
  private boolean enabled = false;
  /**
   * Whether datacenterId and workerId use short bits when applying the local-ip strategy
   * <p>
   * short bits: 4 bits<br>
   * non-short bits: 5 bits
   * <p>
   * Default: false
   */
  private boolean shortDcWkIdBitsWhenUsingLocalIpStrategy = false;

  /**
   * Specified guid number digits(max digits or fixed digits)
   * <p>
   * Default: 19
   */
  private int digits = 19;

  /**
   * Whether the guid digits is fixed
   * <p>
   * Default: false
   */
  private boolean fixedDigitsEnabled = false;

  /**
   * Number of (binary)bits occupied by datacenterId
   * <p>
   * Default: 5
   */
  private long datacenterIdBits = 5;

  /**
   * Number of (binary)bits occupied by workerId
   * <p>
   * Default: 5
   */
  private long workerIdBits = 5;

  /**
   * Number of (binary)bits occupied by sequence
   * <p>
   * Default: 12
   */
  private long seqBits = 12;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    state(enabled, MSG);
    this.enabled = enabled;
  }

  public int getDigits() {
    return digits;
  }

  public void setDigits(int digits) {
    state(enabled, MSG);
    this.digits = digits;
  }

  public boolean isFixedDigitsEnabled() {
    return fixedDigitsEnabled;
  }

  public void setFixedDigitsEnabled(boolean fixedDigitsEnabled) {
    state(enabled, MSG);
    this.fixedDigitsEnabled = fixedDigitsEnabled;
  }

  public long getDatacenterIdBits() {
    return datacenterIdBits;
  }

  public void setDatacenterIdBits(long datacenterIdBits) {
    state(enabled, MSG);
    this.datacenterIdBits = datacenterIdBits;
  }

  public long getWorkerIdBits() {
    return workerIdBits;
  }

  public void setWorkerIdBits(long workerIdBits) {
    state(enabled, MSG);
    this.workerIdBits = workerIdBits;
  }

  public long getSeqBits() {
    return seqBits;
  }

  public void setSeqBits(long seqBits) {
    state(enabled, MSG);
    this.seqBits = seqBits;
  }

  public boolean isShortDcWkIdBitsWhenUsingLocalIpStrategy() {
    return shortDcWkIdBitsWhenUsingLocalIpStrategy;
  }

  public void setShortDcWkIdBitsWhenUsingLocalIpStrategy(boolean shortDcWkIdBitsWhenUsingLocalIpStrategy) {
    state(enabled, MSG);
    this.shortDcWkIdBitsWhenUsingLocalIpStrategy = shortDcWkIdBitsWhenUsingLocalIpStrategy;
  }

}
