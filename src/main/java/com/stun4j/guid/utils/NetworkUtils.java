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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple network util
 * <p>
 * get best available local ip-address<br>
 * be able to pick specified ip by specifying ip-prefix
 * 
 * @author Jay Meng
 */
public final class NetworkUtils {
  private static final Logger LOG = LoggerFactory.getLogger(NetworkUtils.class);
  private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");
  private static volatile InetAddress LOCAL_ADDRESS = null;
  static final String NULL_SAFE_LOCALHOST = "127.0.0.1";
  static final String ANYHOST = "0.0.0.0";

  public static String getLocalHost() {
    return getLocalHost(null);
  }

  public static String getLocalHost(String ipStartith) {
    InetAddress address = getLocalAddress(ipStartith);
    return address == null ? NULL_SAFE_LOCALHOST : address.getHostAddress();
  }

  public static InetAddress getLocalAddress() {
    return getLocalAddress(null);
  }

  public static InetAddress getLocalAddress(String ipStartwith) {
    InetAddress work;
    if ((work = LOCAL_ADDRESS) != null && work.getHostAddress().startsWith(Optional.ofNullable(ipStartwith).orElse("")))
      return work;
    InetAddress localAddress = getLocalAddress0(ipStartwith);
    if (localAddress != null)
      LOCAL_ADDRESS = localAddress;
    return localAddress;
  }

  private static InetAddress getLocalAddress0(String ipStartwith) {
    InetAddress localAddr = null;
    boolean needFilterIp = ipStartwith != null;
    try {
      localAddr = InetAddress.getLocalHost();
      if (isValidAddress(localAddr)) {
        if (needFilterIp && !localAddr.getHostAddress().startsWith(ipStartwith)) {
          return null;
        }
        return localAddr;
      }
    } catch (Exception e) {
      LOG.error("get ip address error, {}", e.getMessage(), e);
    }
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      if (interfaces != null) {
        while (interfaces.hasMoreElements()) {
          try {
            NetworkInterface network = interfaces.nextElement();
            if (network.isLoopback() || network.isVirtual() || !network.isUp()) {
              continue;
            }
            Enumeration<InetAddress> addresses = network.getInetAddresses();
            while (addresses.hasMoreElements()) {
              try {
                InetAddress address = addresses.nextElement();
                if (isValidAddress(address)) {
                  if (needFilterIp && !address.getHostAddress().startsWith(ipStartwith)) {
                    continue;
                  }
                  return address;
                }
              } catch (Exception e) {
                LOG.error("get ip address error, {}", e.getMessage(), e);
              }
            }
          } catch (Exception e) {
            LOG.error("get ip address error, {}", e.getMessage(), e);
          }
        }
      }
    } catch (Exception e) {
      LOG.error("get ip address error, {}", e.getMessage(), e);
    }
    LOG.warn("could not get local host ip address, will use 127.0.0.1 instead");
    return localAddr;
  }

  static boolean isValidAddress(InetAddress inetAddr) {
    if (inetAddr == null || inetAddr.isLoopbackAddress())
      return false;
    String name = inetAddr.getHostAddress();
    return (name != null && !ANYHOST.equals(name) && !NULL_SAFE_LOCALHOST.equals(name)
        && IP_PATTERN.matcher(name).matches());
  }

  private NetworkUtils() {
  }

}
