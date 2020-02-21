/*-
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.stun4j.guid.utils;

// from apache commons-lang3@3.9
public final class StringUtils {

  private StringUtils() {
  }

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
   *
   * @since 3.2
   */
  public static final String SPACE = " ";

  /**
   * The empty String {@code ""}.
   * 
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
   * @param str    the String to pad out, may be null
   * @param size   the size to pad to
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
   *
   * @param ch     character to repeat
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
   * <pre>
   * StringUtils.leftPad(null, *, *)     = null
   * StringUtils.leftPad("", 3, 'z')     = "zzz"
   * StringUtils.leftPad("bat", 3, 'z')  = "bat"
   * StringUtils.leftPad("bat", 5, 'z')  = "zzbat"
   * StringUtils.leftPad("bat", 1, 'z')  = "bat"
   * StringUtils.leftPad("bat", -1, 'z') = "bat"
   * </pre>
   *
   * @param str     the String to pad out, may be null
   * @param size    the size to pad to
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
   *
   * @param cs the CharSequence to check, may be null
   * @return {@code true} if the CharSequence is empty or null
   * @since 3.0 Changed signature from isEmpty(String) to isEmpty(CharSequence)
   */
  public static boolean isEmpty(final CharSequence cs) {
    return cs == null || cs.length() == 0;
  }
}
