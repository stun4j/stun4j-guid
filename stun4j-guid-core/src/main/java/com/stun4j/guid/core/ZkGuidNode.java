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

import static com.stun4j.guid.core.utils.Asserts.state;
import static com.stun4j.guid.core.utils.Exceptions.sneakyThrow;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

import com.stun4j.guid.core.utils.NetworkUtils;
import com.stun4j.guid.core.utils.Strings;
import com.stun4j.guid.core.utils.Utils;
import com.stun4j.guid.core.utils.Utils.Pair;
import com.stun4j.guid.core.utils.Utils.Triple;

/** @author Jay Meng */
public abstract class ZkGuidNode {
  private static final Logger LOG = LoggerFactory.getLogger(ZkGuidNode.class);
  public static final String DFT_ZK_NAMESPACE_GUID = "stun4j-guid";
  private static final String ZK_NODES_PATH_ROOT = "/nodes";
  private static final String ZK_LOCK_PATH_ROOT = "/lock";
  private static final AtomicBoolean STARTED = new AtomicBoolean(false);
  private static CuratorFramework client = null;

  private static int reconnectRetryTimes = 0;

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
    return start(zkConnectStr, onReconnect, zkNamespace, 19, 5, 5, 12, false, ipStartWith);
  }

  public static Pair<Integer, Integer> start(String zkConnectStr, Consumer<Pair<Integer, Integer>> onReconnect,
      String zkNamespace, int digits, int datacenterIdBits, int workerIdBits, int seqBits, boolean fixedDigitsEnabled,
      String ipStartWith) throws Exception {
    // TODO mj:config
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    Builder clientBuilder = CuratorFrameworkFactory.builder().connectString(zkConnectStr).sessionTimeoutMs(5000)
        .connectionTimeoutMs(5000).retryPolicy(retryPolicy)
        .namespace(Optional.ofNullable(zkNamespace).orElse(DFT_ZK_NAMESPACE_GUID));
    return start(clientBuilder, onReconnect, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled,
        ipStartWith);
  }

  public static Pair<Integer, Integer> start(Builder zkClientBuilder, Consumer<Pair<Integer, Integer>> onReconnect,
      String ipStartWith) throws Exception {
    return start(zkClientBuilder, onReconnect, 19, 5, 5, 12, false, ipStartWith);
  }

  public static Pair<Integer, Integer> start(Builder zkClientBuilder, Consumer<Pair<Integer, Integer>> onReconnect,
      int digits, int datacenterIdBits, int workerIdBits, int seqBits, boolean fixedDigitsEnabled, String ipStartWith)
      throws Exception {
    state(STARTED.compareAndSet(false, true), "The guid-node has already been started");
    // LocalGuid preCheck = new LocalGuid(digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled, false);
    LocalGuid preCheck = new LocalGuid(-1, -1, digits, datacenterIdBits, workerIdBits, seqBits, fixedDigitsEnabled,
        false, false);
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
          case RECONNECTED:
            // TODO mj:reconnect behavior appears to be 'serial(concurrency level)', even at different epochs
            // TODO mj:are queued reconnects of different epochs?or we just don't care...
            if (onReconnect == null) return;
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
                Pair<Integer, Integer> newNodeInfo = coreProcess(preCheck, ipStartWith, client, true);
                onReconnect.accept(newNodeInfo);
                break;
              } catch (Throwable e) {
                LOG.error(
                    "The local-guid can't reconnect with zookeeper, which greatly increases the risk of guid-duplication |error: '{}'",
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

      return coreProcess(preCheck, ipStartWith, client, false);

    } catch (Throwable e) {
      Utils.closeQuietly(client);
      throw e;
    }
  }

  private static Pair<Integer, Integer> coreProcess(LocalGuid preChecked, String ipStartWith, CuratorFramework client,
      boolean reconnect) throws Exception {
    String processName = ManagementFactory.getRuntimeMXBean().getName();
    String processId = processName.substring(0, processName.indexOf('@'));
    String selfIp = NetworkUtils.getLocalhost(ipStartWith);
    String selfNodePath = Strings.lenientFormat("%s@%s#", selfIp, processId);
    String selfNodeExpectedZkPath = Strings.lenientFormat("%s/%s", ZK_NODES_PATH_ROOT, selfNodePath);

    ACLBackgroundPathAndBytesable<String> flashWriter = client.create().creatingParentsIfNeeded()
        .withMode(CreateMode.EPHEMERAL);
    // a dbl-check between ip and zk auto-generated ip(data within an empty path)
    String flashCheckPath = "/" + LocalGuid.uuid();
    try {
      flashWriter.forPath(flashCheckPath);
      String ipByZkAutoGen = new String(client.getData().forPath(flashCheckPath));
      if (!selfIp.equals(ipByZkAutoGen)) {
        LOG.warn(
            "Found different ip,ip is a very important identity identifing node-itself,to fix this problem,you may need to specify 'ipStartWith' or please contact administrator [ipByLocal={}], ipByZk={}",
            selfIp, ipByZkAutoGen);
        // TODO mj:error handle strategy,consider throwing exception here
      }
    } finally {
      client.delete().guaranteed().deletingChildrenIfNeeded().inBackground().forPath(flashCheckPath);
    }
    int maxNode = preChecked.getMaxNode();
    long dcIdBits = preChecked.getDatacenterIdBits();
    long wkIdBits = preChecked.getWorkerIdBits();
    /*
     * Use lock to prevent 'phantom read' problem,with lock protected,the threshold '1024-worker-processes' is safely
     * limited
     */
    Triple<Pair<Integer, Integer>, List<String>, String> resultOfCoreProcess = ZkLocks
        .of(client, ZK_LOCK_PATH_ROOT, () -> {
          try {
            List<String> snapshotAllNodes;
            try {
              snapshotAllNodes = client.getChildren().forPath(ZK_NODES_PATH_ROOT);
            } catch (NoNodeException e) {// this is reasonable,other exceptions not accepted
              LOG.warn("Might be the first initialization? [suspected error: {}]", e.getMessage());
              snapshotAllNodes = new ArrayList<>();
            }
            state(snapshotAllNodes.size() < maxNode, "Number of worker-node over limited [max-node=%s]", maxNode);

            ACLBackgroundPathAndBytesable<String> dataWriter = client.create().creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL);
            Pair<Integer, String> nodeInfo = doCreateZkNode(dataWriter, snapshotAllNodes, selfNodeExpectedZkPath,
                maxNode, 0);
            int nodeId = nodeInfo.getKey();
            String nodeZkPath = nodeInfo.getValue();
            state(nodeId >= 0 && nodeId < maxNode, "Wrong guid-node-id [nodeId=%s]", nodeId);

            int datacenterId = (nodeId) >> (int)dcIdBits;
            int workerId = (int)(nodeId & ~(-1L << wkIdBits));

            LOG.info("The guid-node {}started [datacenterId={}, workerId={}, nodeZkPath={}]", !reconnect ? "" : "re",
                datacenterId, workerId, nodeZkPath);

            return Triple.of(Pair.of(datacenterId, workerId), snapshotAllNodes, nodeZkPath);
          } catch (Throwable t) {
            throw sneakyThrow(t);
          }
        }, selfNodeExpectedZkPath).safeRun(15, TimeUnit.SECONDS);

    doCleanUp(client, selfNodePath, resultOfCoreProcess);

    return resultOfCoreProcess.getLeft();
  }

  private static Pair<Integer, String> doCreateZkNode(ACLBackgroundPathAndBytesable<String> dataWriter,
      List<String> snapshotAllNodes, String expectedNodeZkPath, int maxNode, int tryTimes) throws Exception {
    state(tryTimes <= maxNode, "Times of retry over limited [max-retry-times=%s]", maxNode);

    String realNodeZkPath = dataWriter.forPath(expectedNodeZkPath, null);
    int nodeId = calculateNodeIdFrom(realNodeZkPath, maxNode);
    boolean foundDup = snapshotAllNodes.stream()
        .anyMatch(otherNodePath -> nodeId == calculateNodeIdFrom(otherNodePath, maxNode));
    if (foundDup) {
      // TODO mj:error handle
      client.delete().guaranteed().deletingChildrenIfNeeded().inBackground().forPath(realNodeZkPath);
      return doCreateZkNode(dataWriter, snapshotAllNodes, expectedNodeZkPath, maxNode, ++tryTimes);
    }
    return Pair.of(nodeId, realNodeZkPath);
  }

  private static int calculateNodeIdFrom(String nodeZkPath, int maxNode) {
    String last10 = nodeZkPath.substring(nodeZkPath.length() - 10);// TODO mj:always 10d?
    int nodeId = (int)(Long.parseLong(last10) % maxNode);
    return nodeId;
  }

  /**
   * Try remove the old/expired/dirty path
   * <p>
   * In most cases,this means a reconnect happens just after 'suspend' but before the actual 'connection-loss'
   * <p>
   * So for the purpose, more efficient use of '{max-node}-limit', we need to clean-up the old path, after we've got a
   * success node-assignment(which means a new node-path generated)
   */
  private static void doCleanUp(CuratorFramework client, String selfNodePath,
      Triple<Pair<Integer, Integer>, List<String>, String> resultOfCoreProcess) {
    String nodeOldZkPath = null;
    String msgTpl = null;
    List<String> snapshotAllNodes = resultOfCoreProcess.getMiddle();
    String nodeNewZkPath = resultOfCoreProcess.getRight();
    try {
      for (String nodeZkPath : snapshotAllNodes) {
        if (nodeZkPath.startsWith(selfNodePath)) {
          LOG.warn(
              msgTpl = "The guid-node is still alive, the old zk-path '{}' would be replaced with the new zk-path '{}'",
              nodeOldZkPath = ZK_NODES_PATH_ROOT + "/" + nodeZkPath, nodeNewZkPath);
          client.delete().guaranteed().deletingChildrenIfNeeded().inBackground().forPath(nodeOldZkPath);
          break;
        }
      }
    } catch (Throwable e) {
      // swallow any exception of 'old-path-deletion' to guarantee the core/main process
      LOG.warn(msgTpl, nodeOldZkPath, nodeNewZkPath);
    }
  }

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      Utils.closeQuietly(client);
    }));
  }
}
