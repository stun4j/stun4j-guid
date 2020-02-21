package com.stun4j.guid;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * A functional-style template for using zookeeper distributed lock
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