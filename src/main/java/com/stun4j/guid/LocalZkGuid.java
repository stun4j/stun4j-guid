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

import org.apache.curator.framework.CuratorFrameworkFactory.Builder;

import com.stun4j.guid.utils.Pair;

/**
 * Helper class for 'LocalGuid' robustly collaborate with 'ZooKeeper'
 * 
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
    Pair<Integer, Integer> node = ZkGuidNode.start(zkConnectStr, (newNode) -> {
      LocalGuid.instance().reset(newNode);
    }, zkNamespace, ipStartWith);
    return LocalGuid.init(node);
  }

  public static LocalGuid init(Builder zkClientBuilder, String ipStartWith) throws Exception {
    Pair<Integer, Integer> node = ZkGuidNode.start(zkClientBuilder, (newNode) -> {
      LocalGuid.instance().reset(newNode);
    }, ipStartWith);
    return LocalGuid.init(node);
  }
}
