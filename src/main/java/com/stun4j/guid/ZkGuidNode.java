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

import static com.stun4j.guid.utils.Asserts.state;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.CuratorFrameworkFactory.Builder;
import org.apache.curator.framework.api.ACLBackgroundPathAndBytesable;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stun4j.guid.utils.CloseableUtils;
import com.stun4j.guid.utils.NetworkUtils;
import com.stun4j.guid.utils.Pair;
import com.stun4j.guid.utils.Strings;
import com.stun4j.guid.utils.Utils;

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
  
  private static int reconnectRetryTimes = 0;

  @Deprecated
  public static Pair<Integer, Integer> start(String zkConnectStr) throws Exception {
    return start(zkConnectStr, null, null, null);
  }

  @Deprecated
  public static Pair<Integer, Integer> start(String zkConnectStr, String zkNamespace) throws Exception {
    return start(zkConnectStr, null, zkNamespace, null);
  }

  @Deprecated
  public static Pair<Integer, Integer> start(String zkConnectStr, String zkNamespace, String ipStartWith)
      throws Exception {
    return start(zkConnectStr, null, zkNamespace, ipStartWith);
  }

  @Deprecated
  public static Pair<Integer, Integer> start(Builder zkClientBuilder, String ipStartWith) throws Exception {
    return start(zkClientBuilder, null, ipStartWith);
  }

  public static Pair<Integer, Integer> start(String zkConnectStr, Consumer<Pair<Integer, Integer>> onReconnect)
      throws Exception {
    return start(zkConnectStr, onReconnect, null, null);
  }

  public static Pair<Integer, Integer> start(String zkConnectStr, Consumer<Pair<Integer, Integer>> onReconnect,
      String zkNamespace) throws Exception {
    return start(zkConnectStr, onReconnect, zkNamespace, null);
  }

  public static Pair<Integer, Integer> start(String zkConnectStr, Consumer<Pair<Integer, Integer>> onReconnect,
      String zkNamespace, String ipStartWith) throws Exception {
    // TODO mj:config
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    Builder clientBuilder = CuratorFrameworkFactory.builder().connectString(zkConnectStr).sessionTimeoutMs(5000)
        .connectionTimeoutMs(5000).retryPolicy(retryPolicy)
        .namespace(Optional.ofNullable(zkNamespace).orElse(DFT_ZK_NAMESPACE_GUID));
    return start(clientBuilder, onReconnect, ipStartWith);
  }

  // private static final AtomicInteger reconnectEpoch = new AtomicInteger(0);
  // private static final AtomicInteger reconnectRetryTimes = new AtomicInteger(0);
  public static Pair<Integer, Integer> start(Builder zkClientBuilder, Consumer<Pair<Integer, Integer>> onReconnect,
      String ipStartWith) throws Exception {
    state(STARTED.compareAndSet(false, true), "guid-node already started");
    client = zkClientBuilder.build();
    client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
      @Override
      public void stateChanged(CuratorFramework client, ConnectionState newSt) {
        switch (newSt) {
          case LOST:
            // TODO mj:disable guid-generation? behavior->config
            break;
          case SUSPENDED:
            // TODO mj:nothing to do?
            break;
          case RECONNECTED:// TODO mj:reconnect behavior appears to be 'serial(concurrency level)', even at different
                           // epochs
            // TODO mj:are queued reconnects of different epochs?or we just don't care...
            if (onReconnect == null)
              return;
            // int savedEpoch = reconnectEpoch.incrementAndGet();
            while (true) {
              // LOG.info("savedEpoch:{}, currentEpoch:{}", savedEpoch, reconnectEpoch);
              // if (savedEpoch != reconnectEpoch.get()) {
              // //cancel current reconnect,coz found new, this seems to be unnecessary...for the 'serial(concurrency
              // level)' epoch behavior
              // break;
              // }
              if (reconnectRetryTimes >= 60) {// TODO mj:maxRetryTimes->config
                reconnectRetryTimes = 0;// TODO mj:behavior->config
              }
              try {
                Pair<Integer, Integer> newNode = coreProcess(ipStartWith, client, true);
                onReconnect.accept(newNode);
                break;
              } catch (Throwable e) {
                LOG.error(
                    "Local guid can't reconnect with the ZK, which greatly increases the risk of GUID duplication |error: '{}'",
                    e.getMessage(), e);
                Utils.sleepSeconds(reconnectRetryTimes++ % 11);// a simple 'step' sleep time TODO mj:11->config
              }
            }

            break;
          default:
            break;
        }
      }
    });
    try {
      client.start();

      return coreProcess(ipStartWith, client, false);

    } catch (Throwable e) {
      CloseableUtils.closeQuietly(client);
      throw e;
    }
    // TOOD mj:finally?
  }

  private static Pair<Integer, Integer> coreProcess(String ipStartWith, CuratorFramework client, boolean isReconnect)
      throws Exception {
    String processName = ManagementFactory.getRuntimeMXBean().getName();
    String processId = processName.substring(0, processName.indexOf('@'));
    String selfIp = NetworkUtils.getLocalHost(ipStartWith);
    String selfNodePath = Strings.lenientFormat("%s@%s", selfIp, processId);
    String selfNodeFullPath = Strings.lenientFormat("%s/%s", ZK_NODES_PATH_ROOT, selfNodePath);

    // use lock to prevent 'phantom read' problem,with lock protected,the threshold '1024-worker-processes' is safely
    // limited
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
          otherNodes = new ArrayList<>();
        }
        state(otherNodes.size() < MAX_NUM_OF_WORKER_NODE, "number of worker-node over limited [max=%s]",
            MAX_NUM_OF_WORKER_NODE);
        TreeMap<String/* id */, String/* path(preserved, TODO mj:not used) */> nodeIdsIntSorted = new TreeMap<>(
            Comparator.comparingInt((a) -> Integer.parseInt(a)));

        int rtnNodeId = -1;
        for (String otherNodePath : otherNodes) {
          String data = new String(client.getData().forPath(ZK_NODES_PATH_ROOT + "/" + otherNodePath));
          nodeIdsIntSorted.put(data, otherNodePath);

          // try handling special reconnect scenario: node(process) itself still alive->
          int curId = Integer.parseInt(data);
          String lastNodePath = otherNodePath;
          // in most cases,this means a reconnect happens just after 'suspend' but before the actual 'connection-loss'
          // ->
          if (selfNodePath.equals(lastNodePath)) {
            LOG.info("guid-node is still alive, assuming no change [node-path={}, node-id={}]", lastNodePath, curId);
            rtnNodeId = curId;
            break;
          }
          // <-
        }
        // <-

        // try generating node-id
        if (nodeIdsIntSorted.isEmpty()) {
          // during the coordination(zk-side),guid-node-id begin with 1
          String newNodeId = "1";
          dataWriter.forPath(selfNodeFullPath, newNodeId.getBytes());
          rtnNodeId = Integer.parseInt(newNodeId);
        } else {
          if (rtnNodeId < 0) {// 'rtnNodeId<0' indicates that the node(process) itself has vanished(in zk)
            Entry<String, String> lastEntry = null;
            for (Entry<String, String> entry : nodeIdsIntSorted.entrySet()) {
              int curId = Integer.parseInt(entry.getKey());

              int lastId = lastEntry != null ? Integer.parseInt(lastEntry.getKey()) : 0;
              // find a gap,node can be safely inserted
              if (curId > lastId + 1) {
                String newNodeId = String.valueOf(curId - 1);
                dataWriter.forPath(selfNodeFullPath, newNodeId.getBytes());
                rtnNodeId = Integer.parseInt(newNodeId);
                break;
              }
              lastEntry = entry;
            }
          }

          // if nothing matched,a simply increment occurred
          if (rtnNodeId < 0) {
            String maxId = nodeIdsIntSorted.lastKey();
            int newId = Integer.parseInt(maxId) + 1;
            dataWriter.forPath(selfNodeFullPath, String.valueOf(newId).getBytes());
            rtnNodeId = newId;
          }
        }
        state(rtnNodeId > 0 && rtnNodeId <= MAX_NUM_OF_WORKER_NODE, "wrong guid-node-id [nodeId=%s]", rtnNodeId);
        /*
         * the working local-guid-node-id begin with 0,so 'rtnNodeId' has to be decreased by 1,otherwise it works wrong
         */
        String binStr = Strings.leftPad(Integer.toBinaryString(--rtnNodeId), 10, "0");
        String lowAsDatacenterId = binStr.substring(0, 5);
        String highAsWorkerId = binStr.substring(5, 10);
        Integer datacenterId = Integer.valueOf(lowAsDatacenterId, 2);
        Integer workerId = Integer.valueOf(highAsWorkerId, 2);
        LOG.info("guid-node {}started [datacenterId={}, workerId={}, nodePath={}]", !isReconnect ? "" : "re",
            datacenterId, workerId, selfNodePath);
        return Pair.of(datacenterId, workerId);
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }, selfNodeFullPath).safeRun(15, TimeUnit.SECONDS);
  }

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      CloseableUtils.closeQuietly(client);
    }));
  }
}
