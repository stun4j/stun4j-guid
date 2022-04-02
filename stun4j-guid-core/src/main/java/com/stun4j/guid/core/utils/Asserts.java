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

import static com.stun4j.guid.core.utils.Strings.lenientFormat;

import java.util.function.Supplier;

/** @author Jay Meng */
public abstract class Asserts {
  public static <T> void notNull(T reference) {
    if (reference == null) throw new IllegalArgumentException();
  }

  public static <T> void notNull(T reference, String errorMsg) {
    if (reference == null) raiseArgumentException(errorMsg);
  }

  public static <T> void notNull(T reference, Supplier<String> errorMsg) {
    if (reference == null) raiseArgumentException(errorMsg);
  }

  public static void argument(boolean expression, String errorMsg) {
    if (!expression) {
      raiseArgumentException(errorMsg);
    }
  }

  public static void argument(boolean expression, Supplier<String> errorMsg) {
    if (!expression) {
      raiseArgumentException(errorMsg);
    }
  }

  public static void argument(boolean expression, String errorMsgTemplate, Object... errorMsgArgs) {
    if (!expression) {
      raiseArgumentException(lenientFormat(errorMsgTemplate, errorMsgArgs));
    }
  }

  public static void state(boolean expression, String errorMsg) {
    if (!expression) {
      throw new IllegalStateException(errorMsg);
    }
  }

  public static void state(boolean expression, String errorMsgTemplate, Object... errorMsgArgs) {
    if (!expression) {
      throw new IllegalStateException(lenientFormat(errorMsgTemplate, errorMsgArgs));
    }
  }

  public static void state(boolean expression, Supplier<String> errorMsg) {
    if (!expression) {
      throw new IllegalStateException(errorMsg.get());
    }
  }

  private static void raiseArgumentException(String errorMsg) {
    throw new IllegalArgumentException(errorMsg);
  }

  private static void raiseArgumentException(Supplier<String> errorMsg) {
    throw new IllegalArgumentException(errorMsg.get());
  }
}
