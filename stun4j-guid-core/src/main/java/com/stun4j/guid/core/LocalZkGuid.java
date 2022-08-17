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

import java.util.function.Consumer;

import org.apache.curator.framework.CuratorFrameworkFactory.Builder;

import com.stun4j.guid.core.utils.Utils.Pair;

/**
 * Helper class for 'LocalGuid' robustly collaborate with 'ZooKeeper'
 * @author Jay Meng
 */
public class LocalZkGuid {
  public static LocalGuid init(String zkConnectStr) throws Exception {
    return init(zkConnectStr, null, null);
  }

  public static LocalGuid init(String zkConnectStr, String zkNamespace) throws Exception {
    return init(zkConnectStr, zkNamespace, null);
  }

  public static LocalGuid init(String zkConnectStr, String zkNamespace, String ipStartWith) throws Exception {
    return init(zkConnectStr, zkNamespace, 19, 5, 5, 12, false, ipStartWith);
  }

  public static LocalGuid init(Builder zkClientBuilder, String ipStartWith) throws Exception {
    return init(zkClientBuilder, 19, 5, 5, 12, false, ipStartWith);
  }

  private static final Consumer<Pair<Integer, Integer>> ON_RECONNECT_FN = nodeNewInfo -> {
    LocalGuid.instance().reset(nodeNewInfo);
  };

  public static LocalGuid init(String zkConnectStr, int digits, long datacenterIdBits, long workerIdBits, long seqBits,
      boolean fixedDigitsEnabled) throws Exception {
    return init(zkConnectStr, null, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled, null);
  }

  public static LocalGuid init(String zkConnectStr, String zkNamespace, int digits, long datacenterIdBits,
      long workerIdBits, long seqBits, boolean fixedDigitsEnabled, String ipStartWith) throws Exception {
    Pair<Integer, Integer> nodeInfo = ZkGuidNode.start(zkConnectStr, ON_RECONNECT_FN, zkNamespace, digits,
        datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled, ipStartWith);

    return doInit(nodeInfo, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled);
  }

  public static LocalGuid init(Builder zkClientBuilder, int digits, long datacenterIdBits, long workerIdBits,
      long seqBits, boolean fixedDigitsEnabled, String ipStartWith) throws Exception {
    Pair<Integer, Integer> nodeInfo = ZkGuidNode.start(zkClientBuilder, ON_RECONNECT_FN, digits, datacenterIdBits,
        workerIdBits, seqBits, fixedDigitsEnabled, ipStartWith);

    return doInit(nodeInfo, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled);
  }

  private static LocalGuid doInit(Pair<Integer, Integer> nodeInfo, int digits, long datacenterIdBits, long workerIdBits,
      long seqBits, boolean fixedDigitsEnabled) {
    return LocalGuid.init(nodeInfo.getLeft(), nodeInfo.getRight(), digits, datacenterIdBits, workerIdBits, seqBits,
        fixedDigitsEnabled);
  }
}
