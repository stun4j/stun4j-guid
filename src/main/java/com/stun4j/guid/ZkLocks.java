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

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stun4j.guid.utils.Preconditions;

/**
 * A functional-style template for using zookeeper distributed-lock
 * 
 * @author Jay Meng
 */
public class ZkLocks<T> {
  private static final Logger LOG = LoggerFactory.getLogger(ZkLocks.class);
  private final InterProcessMutex lock;
  private final Supplier<T> supplier;
  private final String clientName;

  public static <T> ZkLocks<T> of(CuratorFramework client, String lockPath, Supplier<T> supplier, String clientName) {
    return new ZkLocks<T>(client, lockPath, supplier, clientName);
  }

  ZkLocks(CuratorFramework client, String lockPath, Supplier<T> supplier, String clientName) {
    this.supplier = supplier;
    this.clientName = clientName;
    lock = new InterProcessMutex(client, lockPath);
  }

  public T safeRun(long time, TimeUnit unit) throws Exception {
    Preconditions.checkState(lock.acquire(time, unit), "%s could not acquire the lock", clientName);
    try {
      LOG.debug("{} has the lock", clientName);
      return supplier.get();
    } finally {
      LOG.debug("{} releasing the lock", clientName);
      lock.release(); // always release the lock in a finally block
    }
  }
}