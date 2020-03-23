/*-
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.stun4j.guid.utils;

// from [com.google.guava/guava "28.2-jre"]
/** @author Jay Meng roughly removed '@com.google.common.base.Nullable' */
public final class Preconditions {
  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   *
   * @param expression   a boolean expression
   * @param errorMessage the exception message to use if the check fails; will be converted to a string using
   *                     {@link String#valueOf(Object)}
   * @throws IllegalArgumentException if {@code expression} is false
   */
  public static void checkArgument(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new IllegalArgumentException(String.valueOf(errorMessage));
    }
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   * <p>
   * See {@link #checkArgument(boolean, String, Object...)} for details.
   *
   * @since 20.0 (varargs overload since 2.0)
   */
  public static void checkArgument(boolean b, String errorMessageTemplate, long p1) {
    if (!b) {
      throw new IllegalArgumentException(Strings.lenientFormat(errorMessageTemplate, p1));
    }
  }

  /**
   * Ensures the truth of an expression involving the state of the calling instance, but not involving any parameters to
   * the calling method.
   *
   * @param expression a boolean expression
   * @throws IllegalStateException if {@code expression} is false
   */
  public static void checkState(boolean expression) {
    if (!expression) {
      throw new IllegalStateException();
    }
  }

  /**
   * Ensures the truth of an expression involving the state of the calling instance, but not involving any parameters to
   * the calling method.
   *
   * @param expression   a boolean expression
   * @param errorMessage the exception message to use if the check fails; will be converted to a string using
   *                     {@link String#valueOf(Object)}
   * @throws IllegalStateException if {@code expression} is false
   */
  public static void checkState(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new IllegalStateException(String.valueOf(errorMessage));
    }
  }

  /**
   * Ensures the truth of an expression involving the state of the calling instance, but not involving any parameters to
   * the calling method.
   * <p>
   * See {@link #checkState(boolean, String, Object...)} for details.
   *
   * @since 20.0 (varargs overload since 2.0)
   */
  public static void checkState(boolean b, String errorMessageTemplate, int p1) {
    if (!b) {
      throw new IllegalStateException(Strings.lenientFormat(errorMessageTemplate, p1));
    }
  }

  /**
   * Ensures the truth of an expression involving the state of the calling instance, but not involving any parameters to
   * the calling method.
   * <p>
   * See {@link #checkState(boolean, String, Object...)} for details.
   *
   * @since 20.0 (varargs overload since 2.0)
   */
  public static void checkState(boolean b, String errorMessageTemplate, Object p1) {
    if (!b) {
      throw new IllegalStateException(Strings.lenientFormat(errorMessageTemplate, p1));
    }
  }

  /**
   * Ensures that an object reference passed as a parameter to the calling method is not null.
   *
   * @param reference an object reference
   * @return the non-null reference that was validated
   * @throws NullPointerException if {@code reference} is null
   */
  public static <T> T checkNotNull(T reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
    return reference;
  }

  /**
   * Ensures that an object reference passed as a parameter to the calling method is not null.
   *
   * @param reference    an object reference
   * @param errorMessage the exception message to use if the check fails; will be converted to a string using
   *                     {@link String#valueOf(Object)}
   * @return the non-null reference that was validated
   * @throws NullPointerException if {@code reference} is null
   */
  public static <T> T checkNotNull(T reference, Object errorMessage) {
    if (reference == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
    return reference;
  }

  /**
   * Ensures that an object reference passed as a parameter to the calling method is not null.
   *
   * @param reference            an object reference
   * @param errorMessageTemplate a template for the exception message should the check fail. The message is formed by
   *                             replacing each {@code %s} placeholder in the template with an argument. These are
   *                             matched by position - the first {@code %s} gets {@code
   *     errorMessageArgs[0]} , etc. Unmatched arguments will be appended to the formatted message in square braces.
   *                             Unmatched placeholders will be left as-is.
   * @param errorMessageArgs     the arguments to be substituted into the message template. Arguments are converted to
   *                             strings using {@link String#valueOf(Object)}.
   * @return the non-null reference that was validated
   * @throws NullPointerException if {@code reference} is null
   */
  public static <T> T checkNotNull(T reference, String errorMessageTemplate, Object... errorMessageArgs) {
    if (reference == null) {
      // If either of these parameters is null, the right thing happens anyway
      throw new NullPointerException(Strings.lenientFormat(errorMessageTemplate, errorMessageArgs));
    }
    return reference;
  }

  private Preconditions() {
  }
}