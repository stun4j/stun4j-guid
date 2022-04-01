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

import static com.stun4j.guid.utils.Strings.lenientFormat;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stun4j.guid.IpNotMatchException;

/**
 * A simple network util
 * <p>
 * Get best available local ip-address<br>
 * Be able to pick specified ip by specifying ip-prefix and ip-segment range(only the third segment is supported)
 * @author Jay Meng
 */
public final class NetworkUtils {
  private static final Logger LOG = LoggerFactory.getLogger(NetworkUtils.class);
  private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");
  private static volatile InetAddress LOCAL_ADDRESS = null;
  static final String NULL_SAFE_LOCALHOST = "127.0.0.1";
  static final String ANYHOST = "0.0.0.0";

  public static String getLocalhost() {
    return getLocalhost(null);
  }

  public static String getLocalhost(String ipStartWith, int... theThirdSegmentRange) throws IpNotMatchException {
    InetAddress address = doGetLocalAddress(ipStartWith, theThirdSegmentRange);
    String rtn = address == null ? NULL_SAFE_LOCALHOST : address.getHostAddress();
    if (!isIpMatch(() -> rtn, ipStartWith, theThirdSegmentRange)) {
      // exception throw is a very strict manner
      throw new IpNotMatchException(lenientFormat(
          "Got unexpected local-ip-address, expect ip (start-with '%s' and third-seg-range %s), but the actual ip is '%s'", //
          ipStartWith, //
          (theThirdSegmentRange == null || theThirdSegmentRange.length == 0) ? "ignored"//
              : "within " + Arrays.toString(theThirdSegmentRange), //
          rtn));
    }
    return rtn;
  }

  private synchronized static InetAddress doGetLocalAddress(String ipStartWith, int... theThirdSegmentRange) {
    if (ipStartWith == null) {
      Asserts.argument(theThirdSegmentRange == null || theThirdSegmentRange.length == 0,
          "Only ip-segment checking is not supported yet");
    }
    // If the ip dosen't match the pattern,the local cache would be ignored
    InetAddress work;
    if ((work = LOCAL_ADDRESS) != null && isIpMatch(() -> work.getHostAddress(), ipStartWith, theThirdSegmentRange)) {
      return work;
    }
    // Cache the ip if found available
    InetAddress addr = doGetLocalAddress0(ipStartWith, theThirdSegmentRange);
    if (addr != null) {
      boolean needFilterIp = ipStartWith != null;
      if (!needFilterIp) {
        LOCAL_ADDRESS = addr;
      } else if (isIpMatch(() -> addr.getHostAddress(), ipStartWith, theThirdSegmentRange)) {
        LOCAL_ADDRESS = addr;
      }
    }
    return addr;
  }

  private static boolean isIpMatch(Supplier<String> addrProvider, String ipStartwith, int... theThirdSegmentRange) {
    boolean needFilterIp = ipStartwith != null;
    if (!needFilterIp) {
      return true;
    }
    String addrStr;
    if (!(addrStr = addrProvider.get()).startsWith(ipStartwith)) {
      return false;
    }
    if (theThirdSegmentRange == null || theThirdSegmentRange.length == 0) {
      return true;
    }
    String[] tmp;
    if ((tmp = addrStr.split("\\.")).length != 4) {
      return false;
    }
    try {
      return Arrays.binarySearch(theThirdSegmentRange, Integer.parseInt(tmp[2])) >= 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private static InetAddress doGetLocalAddress0(String ipStartWith, int... theThirdSegmentRange) {
    InetAddress fastAddr;
    boolean needFilterIp = ipStartWith != null;
    try {
      // fast pick
      if (isValidAddress(fastAddr = InetAddress.getLocalHost())) {
        if (!needFilterIp) {
          return fastAddr;
        }
        if (isIpMatch(() -> fastAddr.getHostAddress(), ipStartWith, theThirdSegmentRange)) {
          return fastAddr;
        }
      }
      // slow pick
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      if (interfaces == null) {
        LOG.warn("No network-interface found, will use '{}' instead [local-ip-address={}]", NULL_SAFE_LOCALHOST,
            fastAddr);
        return null;
      }
      while (interfaces.hasMoreElements()) {
        NetworkInterface network = null;
        try {
          network = interfaces.nextElement();
          if (network.isLoopback() || network.isVirtual() || !network.isUp()) {
            continue;
          }
          Enumeration<InetAddress> addresses = network.getInetAddresses();
          while (addresses.hasMoreElements()) {
            try {
              InetAddress slowAddr = addresses.nextElement();
              if (isValidAddress(slowAddr)) {
                if (!needFilterIp) {
                  return slowAddr;
                }
                if (isIpMatch(() -> slowAddr.getHostAddress(), ipStartWith, theThirdSegmentRange)) {
                  return slowAddr;
                }
                continue;
              }
            } catch (Exception e) {
              LOG.debug("Get local-ip-address error, trying next address [current-network-interface='{}'] |error: '{}'",
                  network, e.getMessage());
            }
          }
        } catch (Exception e) {
          LOG.debug(
              "Get local-ip-address error, trying next network-interface [current-network-interface='{}'] |error: '{}'",
              network, e.getMessage());
        }
      }
    } catch (Exception e) {
      LOG.debug("Get local-ip-address error, will use '{}' instead", NULL_SAFE_LOCALHOST, e);
      return null;
    }

    if (!isValidAddress(fastAddr)) {
      LOG.warn("Invalid local-ip-address '{}', will use '{}' instead", fastAddr, NULL_SAFE_LOCALHOST);
      return null;
    }
    return fastAddr;// this indicates that the address may not match the filter
  }

  static boolean isValidAddress(InetAddress addr) {
    if (addr == null || addr.isLoopbackAddress()) return false;
    String addrStr = addr.getHostAddress();
    return (addrStr != null && !ANYHOST.equals(addrStr) && !NULL_SAFE_LOCALHOST.equals(addrStr)
        && IP_PATTERN.matcher(addrStr).matches());
  }

  private NetworkUtils() {
  }
}
