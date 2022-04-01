/*
 * Copyright 2020-2022 the original author or authors.
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

import static java.util.logging.Level.WARNING;

import java.util.logging.Logger;

public final class Strings {
  // from [com.google.guava/guava "28.2-jre"]
  // Copyright (C) 2010 The Guava Authors------------------->
  /**
   * Returns the given {@code template} string with each occurrence of {@code "%s"} replaced with the corresponding
   * argument value from {@code args}; or, if the placeholder and argument counts do not match, returns a best-effort
   * form of that string. Will not throw an exception under normal conditions.
   * <p>
   * <b>Note:</b> For most string-formatting needs, use {@link String#format String.format},
   * {@link java.io.PrintWriter#format PrintWriter.format}, and related methods. These support the full range of
   * <a href="https://docs.oracle.com/javase/9/docs/api/java/util/Formatter.html#syntax">format specifiers</a>, and
   * alert you to usage errors by throwing {@link java.util.IllegalFormatException}.
   * <p>
   * In certain cases, such as outputting debugging information or constructing a message to be used for another
   * unchecked exception, an exception during string formatting would serve little purpose except to supplant the real
   * information you were trying to provide. These are the cases this method is made for; it instead generates a
   * best-effort string with all supplied argument values present. This method is also useful in environments such as
   * GWT where {@code
   * String.format} is not available. As an example, method implementations of the {@link Preconditions} class use this
   * formatter, for both of the reasons just discussed.
   * <p>
   * <b>Warning:</b> Only the exact two-character placeholder sequence {@code "%s"} is recognized.
   * @param template a string containing zero or more {@code "%s"} placeholder sequences. {@code
   *     null} is treated as the four-character string {@code "null"}.
   * @param args the arguments to be substituted into the message template. The first argument specified is
   *        substituted for the first occurrence of {@code "%s"} in the template, and so forth. A {@code null}
   *        argument is converted to the four-character string {@code "null"}; non-null values are converted to
   *        strings using {@link Object#toString()}.
   * @since 25.1
   */
  // TODO(diamondm) consider using Arrays.toString() for array parameters
  public static String lenientFormat(String template, Object... args) {
    template = String.valueOf(template); // null -> "null"

    if (args == null) {
      args = new Object[]{"(Object[])null"};
    } else {
      for (int i = 0; i < args.length; i++) {
        args[i] = lenientToString(args[i]);
      }
    }

    // start substituting the arguments into the '%s' placeholders
    StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
    int templateStart = 0;
    int i = 0;
    while (i < args.length) {
      int placeholderStart = template.indexOf("%s", templateStart);
      if (placeholderStart == -1) {
        break;
      }
      builder.append(template, templateStart, placeholderStart);
      builder.append(args[i++]);
      templateStart = placeholderStart + 2;
    }
    builder.append(template, templateStart, template.length());

    // if we run out of placeholders, append the extra args in square braces
    if (i < args.length) {
      builder.append(" [");
      builder.append(args[i++]);
      while (i < args.length) {
        builder.append(", ");
        builder.append(args[i++]);
      }
      builder.append(']');
    }

    return builder.toString();
  }

  private static String lenientToString(Object o) {
    try {
      return String.valueOf(o);
    } catch (Exception e) {
      // Default toString() behavior - see Object.toString()
      String objectToString = o.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(o));
      // Logger is created inline with fixed name to avoid forcing Proguard to create another class.
      Logger.getLogger("com.stun4j.guid.utils.StringUtils").log(WARNING,
          "Exception during lenientFormat for " + objectToString, e);
      return "<" + objectToString + " threw " + e.getClass().getName() + ">";
    }
  }

  // from [org.apache.commons/commons-lang3 "3.9"]------------------->

  // Performance testing notes (JDK 1.4, Jul03, scolebourne)
  // Whitespace:
  // Character.isWhitespace() is faster than WHITESPACE.indexOf()
  // where WHITESPACE is a string of all whitespace characters
  //
  // Character access:
  // String.charAt(n) versus toCharArray(), then array[n]
  // String.charAt(n) is about 15% worse for a 10K string
  // They are about equal for a length 50 string
  // String.charAt(n) is about 4 times better for a length 3 string
  // String.charAt(n) is best bet overall
  //
  // Append:
  // String.concat about twice as fast as StringBuffer.append
  // (not sure who tested this)

  /**
   * A String for a space character.
   * @since 3.2
   */
  public static final String SPACE = " ";

  /**
   * The empty String {@code ""}.
   * @since 2.0
   */
  public static final String EMPTY = "";

  /**
   * <p>
   * The maximum size to which the padding constant(s) can expand.
   * </p>
   */
  private static final int PAD_LIMIT = 8192;

  /**
   * <p>
   * Left pad a String with a specified String.
   * </p>
   * <p>
   * Pad to a size of {@code size}.
   * </p>
   * 
   * <pre>
   * StringUtils.leftPad(null, *, *)      = null
   * StringUtils.leftPad("", 3, "z")      = "zzz"
   * StringUtils.leftPad("bat", 3, "yz")  = "bat"
   * StringUtils.leftPad("bat", 5, "yz")  = "yzbat"
   * StringUtils.leftPad("bat", 8, "yz")  = "yzyzybat"
   * StringUtils.leftPad("bat", 1, "yz")  = "bat"
   * StringUtils.leftPad("bat", -1, "yz") = "bat"
   * StringUtils.leftPad("bat", 5, null)  = "  bat"
   * StringUtils.leftPad("bat", 5, "")    = "  bat"
   * </pre>
   *
   * @param str the String to pad out, may be null
   * @param size the size to pad to
   * @param padStr the String to pad with, null or empty treated as single space
   * @return left padded String or original String if no padding is necessary, {@code null} if null String input
   */
  public static String leftPad(final String str, final int size, String padStr) {
    if (str == null) {
      return null;
    }
    if (isEmpty(padStr)) {
      padStr = SPACE;
    }
    final int padLen = padStr.length();
    final int strLen = str.length();
    final int pads = size - strLen;
    if (pads <= 0) {
      return str; // returns original String when possible
    }
    if (padLen == 1 && pads <= PAD_LIMIT) {
      return leftPad(str, size, padStr.charAt(0));
    }

    if (pads == padLen) {
      return padStr.concat(str);
    } else if (pads < padLen) {
      return padStr.substring(0, pads).concat(str);
    } else {
      final char[] padding = new char[pads];
      final char[] padChars = padStr.toCharArray();
      for (int i = 0; i < pads; i++) {
        padding[i] = padChars[i % padLen];
      }
      return new String(padding).concat(str);
    }
  }

  /**
   * <p>
   * Returns padding using the specified delimiter repeated to a given length.
   * </p>
   * 
   * <pre>
   * StringUtils.repeat('e', 0)  = ""
   * StringUtils.repeat('e', 3)  = "eee"
   * StringUtils.repeat('e', -2) = ""
   * </pre>
   * <p>
   * Note: this method does not support padding with
   * <a href="http://www.unicode.org/glossary/#supplementary_character">Unicode Supplementary Characters</a> as they
   * require a pair of {@code char}s to be represented. If you are needing to support full I18N of your applications
   * consider using {@link #repeat(String, int)} instead.
   * </p>
   * @param ch character to repeat
   * @param repeat number of times to repeat char, negative treated as zero
   * @return String with repeated character
   * @see #repeat(String, int)
   */
  public static String repeat(final char ch, final int repeat) {
    if (repeat <= 0) {
      return EMPTY;
    }
    final char[] buf = new char[repeat];
    for (int i = repeat - 1; i >= 0; i--) {
      buf[i] = ch;
    }
    return new String(buf);
  }

  /**
   * <p>
   * Left pad a String with a specified character.
   * </p>
   * <p>
   * Pad to a size of {@code size}.
   * </p>
   * 
   * <pre>
   * StringUtils.leftPad(null, *, *)     = null
   * StringUtils.leftPad("", 3, 'z')     = "zzz"
   * StringUtils.leftPad("bat", 3, 'z')  = "bat"
   * StringUtils.leftPad("bat", 5, 'z')  = "zzbat"
   * StringUtils.leftPad("bat", 1, 'z')  = "bat"
   * StringUtils.leftPad("bat", -1, 'z') = "bat"
   * </pre>
   *
   * @param str the String to pad out, may be null
   * @param size the size to pad to
   * @param padChar the character to pad with
   * @return left padded String or original String if no padding is necessary, {@code null} if null String input
   * @since 2.0
   */
  public static String leftPad(final String str, final int size, final char padChar) {
    if (str == null) {
      return null;
    }
    final int pads = size - str.length();
    if (pads <= 0) {
      return str; // returns original String when possible
    }
    if (pads > PAD_LIMIT) {
      return leftPad(str, size, String.valueOf(padChar));
    }
    return repeat(padChar, pads).concat(str);
  }

  // Empty checks
  // -----------------------------------------------------------------------
  /**
   * <p>
   * Checks if a CharSequence is empty ("") or null.
   * </p>
   * 
   * <pre>
   * StringUtils.isEmpty(null)      = true
   * StringUtils.isEmpty("")        = true
   * StringUtils.isEmpty(" ")       = false
   * StringUtils.isEmpty("bob")     = false
   * StringUtils.isEmpty("  bob  ") = false
   * </pre>
   * <p>
   * NOTE: This method changed in Lang version 2.0. It no longer trims the CharSequence. That functionality is available
   * in isBlank().
   * </p>
   * @param cs the CharSequence to check, may be null
   * @return {@code true} if the CharSequence is empty or null
   * @since 3.0 Changed signature from isEmpty(String) to isEmpty(CharSequence)
   */
  public static boolean isEmpty(final CharSequence cs) {
    return cs == null || cs.length() == 0;
  }

  private Strings() {
  }
}
