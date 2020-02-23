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

import java.lang.management.ManagementFactory;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.CuratorFrameworkFactory.Builder;
import org.apache.curator.framework.api.ACLBackgroundPathAndBytesable;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.shaded.com.google.common.collect.Lists;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stun4j.guid.utils.CloseableUtils;
import com.stun4j.guid.utils.NetworkUtils;
import com.stun4j.guid.utils.Pair;
import com.stun4j.guid.utils.Preconditions;
import com.stun4j.guid.utils.Strings;

/** @author Jay Meng */
public abstract class ZkGuidNode {
  private static final Logger LOG = LoggerFactory.getLogger(ZkGuidNode.class);
  private static final int MAX_NUM_OF_WORKER_NODE = 1024;
  private static final String DFT_ZK_NAMESPACE_GUID = "stun4j-guid";
  private static final String ZK_NODES_PATH_ROOT = "/nodes";
  private static final String ZK_LOCK_PATH_ROOT = "/lock";
  private static final AtomicBoolean STARTED = new AtomicBoolean(false);
  // TODO mj:registry abstraction extract,prevent curator coupling
  private static CuratorFramework client = null;

  public static Pair<Integer, Integer> start(String zkConnectStr) throws Exception {
    return start(zkConnectStr, null, null);
  }

  public static Pair<Integer, Integer> start(String zkConnectStr, String zkNamespace) throws Exception {
    return start(zkConnectStr, zkNamespace, null);
  }

  public static Pair<Integer, Integer> start(String zkConnectStr, String zkNamespace, String ipStartWith)
      throws Exception {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    Builder clientBuilder = CuratorFrameworkFactory.builder().connectString(zkConnectStr).sessionTimeoutMs(5000)
        .connectionTimeoutMs(5000).retryPolicy(retryPolicy)
        .namespace(Optional.ofNullable(zkNamespace).orElse(DFT_ZK_NAMESPACE_GUID));
    return start(clientBuilder, ipStartWith);
  }

  public static Pair<Integer, Integer> start(Builder zkClientBuilder, String ipStartWith) throws Exception {
    Preconditions.checkState(STARTED.compareAndSet(false, true), "guid-node already started");
    client = zkClientBuilder.build();
    try {
      client.start();

      String processName = ManagementFactory.getRuntimeMXBean().getName();
      String processId = processName.substring(0, processName.indexOf('@'));
      String selfIp = NetworkUtils.getLocalHost(ipStartWith);
      String selfNodePath = Strings.lenientFormat("%s/%s@%s", ZK_NODES_PATH_ROOT, selfIp, processId);

      // use lock to prevent 'phantom read' problem,with lock protected,the threshold '1024' should be safe
      return ZkLocks.of(client, ZK_LOCK_PATH_ROOT, () -> {
        try {
          // building a full-snapshot of all the cluster members->
          ACLBackgroundPathAndBytesable<String> dataWriter = client.create().creatingParentsIfNeeded()
              .withMode(CreateMode.EPHEMERAL);
          // a dbl-check between ip and zk auto-generated ip(data within an empty path)->
          String flashCheckPath = "/" + LocalGuid.uuid();
          try {
            dataWriter.forPath(flashCheckPath);
            String ipByZkAutoGen = new String(client.getData().forPath(flashCheckPath));
            if (!selfIp.equals(ipByZkAutoGen)) {
              LOG.warn(
                  "found different ip,ip is a very important identity identifing node-itself,to fix this problem,you may need to specify 'ipStartWith' or please contact administrator [ipByLocal={}], ipByZk={}",
                  selfIp, ipByZkAutoGen);
              // TODO mj:error handle strategy,consider throwing exception here
            }
          } finally {
            client.delete().guaranteed().deletingChildrenIfNeeded().inBackground().forPath(flashCheckPath);
          }
          // <-

          List<String> otherNodes;
          try {
            otherNodes = client.getChildren().forPath(ZK_NODES_PATH_ROOT);
          } catch (NoNodeException e) {// this is reasonable,other exceptions not accepted
            LOG.warn("might be the first initialization? [suspected err: {}]", e.getMessage());
            otherNodes = Lists.newArrayList();
          }
          Preconditions.checkState(otherNodes.size() < MAX_NUM_OF_WORKER_NODE,
              "number of worker-node over limited [max=%s]", MAX_NUM_OF_WORKER_NODE);
          TreeMap<String/* id */, String/* path(preserved, TODO mj:not used) */> nodeIdsIntSorted = new TreeMap<>(
              Comparator.comparingInt((a) -> Integer.parseInt(a)));
          for (String otherNodePath : otherNodes) {
            String data = new String(client.getData().forPath(ZK_NODES_PATH_ROOT + "/" + otherNodePath));
            nodeIdsIntSorted.put(data, otherNodePath);
          }
          // <-

          // try generating node-id
          int rtnNodeId = -1;
          if (nodeIdsIntSorted.isEmpty()) {
            // during the coordination(zk-side),guid-node-id begin with 1
            String newNodeId = "1";
            dataWriter.forPath(selfNodePath, newNodeId.getBytes());
            rtnNodeId = Integer.parseInt(newNodeId);
          } else {
            Entry<String, String> lastEntry = null;
            for (Entry<String, String> entry : nodeIdsIntSorted.entrySet()) {
              int curId = Integer.parseInt(entry.getKey());
              int lastId = lastEntry != null ? Integer.parseInt(lastEntry.getKey()) : 0;
              // find a gap,node can be safely inserted
              if (curId > lastId + 1) {
                String newNodeId = String.valueOf(curId - 1);
                dataWriter.forPath(selfNodePath, newNodeId.getBytes());
                rtnNodeId = Integer.parseInt(newNodeId);
                break;
              }
              lastEntry = entry;
            }
            // if nothing matched,a simply increment occurred
            if (rtnNodeId < 0) {
              String maxId = nodeIdsIntSorted.lastKey();
              int newId = Integer.parseInt(maxId) + 1;
              dataWriter.forPath(selfNodePath, String.valueOf(newId).getBytes());
              rtnNodeId = newId;
            }
          }
          Preconditions.checkState(rtnNodeId > 0 && rtnNodeId <= MAX_NUM_OF_WORKER_NODE,
              "wrong guid-node-id [nodeId=%s]", rtnNodeId);
          /*
           * the working local-guid-node-id begin with 0,so 'rtnNodeId' has to be decreased by 1,otherwise it works
           * wrong
           */
          String binStr = Strings.leftPad(Integer.toBinaryString(--rtnNodeId), 10, "0");
          String lowAsDatacenterId = binStr.substring(0, 5);
          String highAsWorkerId = binStr.substring(5, 10);
          Integer datacenterId = Integer.valueOf(lowAsDatacenterId, 2);
          Integer workerId = Integer.valueOf(highAsWorkerId, 2);
          LOG.info("guid-node started [datacenterId={}, workerId={}]", datacenterId, workerId);
          return Pair.of(datacenterId, workerId);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }, selfNodePath).safeRun(15, TimeUnit.SECONDS);

    } catch (Exception e) {
      CloseableUtils.closeQuietly(client);
      throw e;
    }
  }

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      CloseableUtils.closeQuietly(client);
    }));
  }
}
