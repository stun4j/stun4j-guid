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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Base class for configuration of Guid.
 * @author Jay Meng
 */
@ConfigurationProperties("stun4j.guid")
public class GuidProperties {
  private int datacenterId;
  private int workerId;
  private String ipStartWith;
  /** Default: local-ip */
  private Strategy strategy = Strategy.LOCAL_IP;
  private String zkConnAddr = "localhost:2181";
  private String zkNamespace = "stun4j-guid";

  public enum Strategy {
    ZK, LOCAL_IP, MANUAL
  }

  @NestedConfigurationProperty
  private BitEditing bitEditing = new BitEditing();

  @NestedConfigurationProperty
  private Multiton multiton = new Multiton();

  public int getDatacenterId() {
    return datacenterId;
  }

  public void setDatacenterId(int datacenterId) {
    this.datacenterId = datacenterId;
  }

  public int getWorkerId() {
    return workerId;
  }

  public void setWorkerId(int workerId) {
    this.workerId = workerId;
  }

  public String getIpStartWith() {
    return ipStartWith;
  }

  public void setIpStartWith(String ipStartWith) {
    this.ipStartWith = ipStartWith;
  }

  public Strategy getStrategy() {
    return strategy;
  }

  public void setStrategy(Strategy strategy) {
    this.strategy = strategy;
  }

  public String getZkConnAddr() {
    return zkConnAddr;
  }

  public void setZkConnAddr(String zkConnAddr) {
    this.zkConnAddr = zkConnAddr;
  }

  public String getZkNamespace() {
    return zkNamespace;
  }

  public void setZkNamespace(String zkNamespace) {
    this.zkNamespace = zkNamespace;
  }

  public BitEditing getBitEditing() {
    return bitEditing;
  }

  public Multiton getMultiton() {
    return multiton;
  }

  @Override
  public String toString() {
    return "GuidProperties [datacenterId=" + datacenterId + ", workerId=" + workerId + ", ipStartWith=" + ipStartWith
        + ", strategy=" + strategy + ", zkConnAddr=" + zkConnAddr + ", zkNamespace=" + zkNamespace + ", bitEditing="
        + bitEditing + ", multiton=" + multiton + "]";
  }

}
