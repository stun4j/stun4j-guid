package com.stun4j.guid.utils;

import static com.stun4j.guid.utils.Strings.lenientFormat;

import java.util.function.Supplier;

/** @author Jay Meng */
public abstract class Asserts {
  public static <T> void notNull(T reference) {
    if (reference == null)
      throw new IllegalArgumentException();
  }

  public static <T> void notNull(T reference, String errorMsg) {
    if (reference == null)
      raiseArgumentException(errorMsg);
  }

  public static <T> void notNull(T reference, Supplier<String> errorMsg) {
    if (reference == null)
      raiseArgumentException(errorMsg);
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
