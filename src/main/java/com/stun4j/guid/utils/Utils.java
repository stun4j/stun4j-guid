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
package com.stun4j.guid.utils;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Jay Meng */
public final class Utils {
  private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

  private static final int NANOS_PER_MS = 1000 * 1000;
  private static final int NANOS_PER_SECONDS = 1000 * NANOS_PER_MS;

  public static void sleepMs(long ms) {
    LockSupport.parkNanos(ms * NANOS_PER_MS);
  }

  public static void sleepSeconds(long seconds) {
    LockSupport.parkNanos(seconds * NANOS_PER_SECONDS);
  }

  public static <T> Pair<Long, T> timeAwareRun(Supplier<T> supplier) {
    long start = System.currentTimeMillis();
    T rtn = supplier.get();
    return Pair.of(System.currentTimeMillis() - start, rtn);
  }

  public static <T, R> Pair<Long, R> timeAwareRun(Function<T, R> fn, T arg) {
    long start = System.currentTimeMillis();
    R rtn = fn.apply(arg);
    return Pair.of(System.currentTimeMillis() - start, rtn);
  }

  /**
   * from curator-recipes:2.13.0
   * <p>
   * This method has been added because Guava has removed the {@code closeQuietly()} method from {@code Closeables} in
   * v16.0. It's tempting simply to replace calls to {@code closeQuietly(closeable)} with calls to
   * {@code close(closeable, true)} to close {@code Closeable}s while swallowing {@code IOException}s, but
   * {@code close()} is declared as {@code throws IOException} whereas {@code closeQuietly()} is not, so it's not a
   * drop-in replacement.
   * </p>
   * <p>
   * On the whole, Guava is very backwards compatible. By fixing this nit, Curator can continue to support newer
   * versions of Guava without having to bump its own dependency version.
   * </p>
   * <p>
   * See <a href="https://issues.apache.org/jira/browse/CURATOR-85">https://issues.apache.org/jira/browse/CURATOR-85</a>
   * </p>
   */
  public static void closeQuietly(Closeable closeable) {
    try {
      // Here we've instructed Guava to swallow the IOException
      Closeables.close(closeable, true);
    } catch (IOException e) {
      // We instructed Guava to swallow the IOException, so this should
      // never happen. Since it did, log it.
      LOG.error("IOException should not have been thrown.", e);
    }
  }

  private Utils() {
  }

  public static final class Pair<L, R> {
    private final L left;
    private final R right;

    public static <L, R> Pair<L, R> of(L left, R right) {
      return new Pair<>(left, right);
    }

    public Pair(L left, R right) {
      this.left = left;
      this.right = right;
    }

    public L getLeft() {
      return left;
    }

    public R getRight() {
      return right;
    }

    public L getKey() {
      return left;
    }

    public R getValue() {
      return right;
    }

    @SuppressWarnings("unused")
    private Pair() {
      this.left = null;
      this.right = null;
    }

    @Override
    public String toString() {
      return "Pair [left=" + left + ", right=" + right + "]";
    }

  }

  public static final class Triple<L, M, R> {
    private final L left;
    private final M middle;
    private final R right;

    public static <L, M, R> Triple<L, M, R> of(L left, M middle, R right) {
      return new Triple<>(left, middle, right);
    }

    public Triple(L left, M middle, R right) {
      this.left = left;
      this.middle = middle;
      this.right = right;
    }

    public L getLeft() {
      return left;
    }

    public M getMiddle() {
      return middle;
    }

    public R getRight() {
      return right;
    }

    @SuppressWarnings("unused")
    private Triple() {
      this.left = null;
      this.middle = null;
      this.right = null;
    }

    @Override
    public String toString() {
      return "Triple [left=" + left + ", middle=" + middle + ", right=" + right + "]";
    }

  }
}
