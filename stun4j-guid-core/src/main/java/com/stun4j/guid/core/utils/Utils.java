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
package com.stun4j.guid.core.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Calendar;
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

  public static long yearsLongMillis(int years) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(0);
    cal.add(Calendar.YEAR, years);
    return cal.getTimeInMillis();
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

  /**
   * Utility methods for working with {@link Closeable} objects.
   * <p>
   * From guava:31.1-jre,changes listed below
   * <ul>
   * <li>Disable all guava-specific annotations,use slf4j logger instead</li>
   * </ul>
   * @author Michael Lancaster
   * @author Jay Meng
   * @since 1.0
   */
  // @Beta
  // @GwtIncompatible
  // @ElementTypesAreNonnullByDefault
  public static final class Closeables {
    // @VisibleForTesting
    // static final Logger LOG = LoggerFactory.getLogger(Closeables.class);

    private Closeables() {
    }

    /**
     * Closes a {@link Closeable}, with control over whether an {@code IOException} may be thrown.
     * This is primarily useful in a finally block, where a thrown exception needs to be logged but
     * not propagated (otherwise the original exception will be lost).
     * <p>
     * If {@code swallowIOException} is true then we never throw {@code IOException} but merely log
     * it.
     * <p>
     * Example:
     *
     * <pre>
     * {@code
     * public void useStreamNicely() throws IOException {
     *   SomeStream stream = new SomeStream("foo");
     *   boolean threw = true;
     *   try {
     *     // ... code which does something with the stream ...
     *     threw = false;
     *   } finally {
     *     // If an exception occurs, rethrow it only if threw==false:
     *     Closeables.close(stream, threw);
     *   }
     * }
     * }
     * </pre>
     *
     * @param closeable the {@code Closeable} object to be closed, or null, in which case this method
     *        does nothing
     * @param swallowIOException if true, don't propagate IO exceptions thrown by the {@code close}
     *        methods
     * @throws IOException if {@code swallowIOException} is false and {@code close} throws an {@code
     *     IOException}.
     */
    public static void close(Closeable closeable, boolean swallowIOException) throws IOException {
      if (closeable == null) {
        return;
      }
      try {
        closeable.close();
      } catch (IOException e) {
        if (swallowIOException) {
          // logger.log(Level.WARNING, "IOException thrown while closing Closeable.", e);
          LOG.warn("IOException thrown while closing Closeable.", e);
        } else {
          throw e;
        }
      }
    }

    /**
     * Closes the given {@link InputStream}, logging any {@code IOException} that's thrown rather than
     * propagating it.
     * <p>
     * While it's not safe in the general case to ignore exceptions that are thrown when closing an
     * I/O resource, it should generally be safe in the case of a resource that's being used only for
     * reading, such as an {@code InputStream}. Unlike with writable resources, there's no chance that
     * a failure that occurs when closing the stream indicates a meaningful problem such as a failure
     * to flush all bytes to the underlying resource.
     * @param inputStream the input stream to be closed, or {@code null} in which case this method
     *        does nothing
     * @since 17.0
     */
    public static void closeQuietly(InputStream inputStream) {
      try {
        close(inputStream, true);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    /**
     * Closes the given {@link Reader}, logging any {@code IOException} that's thrown rather than
     * propagating it.
     * <p>
     * While it's not safe in the general case to ignore exceptions that are thrown when closing an
     * I/O resource, it should generally be safe in the case of a resource that's being used only for
     * reading, such as a {@code Reader}. Unlike with writable resources, there's no chance that a
     * failure that occurs when closing the reader indicates a meaningful problem such as a failure to
     * flush all bytes to the underlying resource.
     * @param reader the reader to be closed, or {@code null} in which case this method does nothing
     * @since 17.0
     */
    public static void closeQuietly(Reader reader) {
      try {
        close(reader, true);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
  }
}
