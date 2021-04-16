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

/** @author Jay Meng */
public final class Triple<L, M, R> {
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
