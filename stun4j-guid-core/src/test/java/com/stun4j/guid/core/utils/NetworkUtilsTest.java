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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

import com.stun4j.guid.core.IpNotMatchException;
import com.stun4j.guid.core.utils.NetworkUtils;
import com.stun4j.guid.core.utils.Strings;

public class NetworkUtilsTest {
  static boolean isNetworkUp() {
    if (isNetworkUp0()) return true;
    return isNetworkUp1();
  }

  // Not applicable to Intranet environments (Intranet that cannot access the Internet)
  static boolean isNetworkUp0() {
    Process ps = null;
    try {
      ps = Runtime.getRuntime().exec("ping baidu.com");
      try (BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()))) {
        String line = br.readLine();
        System.out.println("net echo: " + line);
        return line != null;
      }
    } catch (Exception e) {
      return false;
    } finally {
      try {
        ps.destroyForcibly();
      } catch (Exception e) {
      }
    }
  }

  // Expect to support both WAN and LAN
  static boolean isNetworkUp1() {
    String ip = tryGetIpAnotherWay("en0", "en1");
    if (ip != null) {
      return true;
    }
    ip = tryGetIpAnotherWay("lo0");
    System.out.println("It seems that network is down, loopback ip is " + ip);
    return false;
  }

  // Might only works on my machine(MBP with 2 network interfaces,one is wired,another one is wireless)
  static String tryGetIpAnotherWay(String... networkInterfaces) {
    // String[] nis = new String[]{"en0", "en1"};
    for (String ni : networkInterfaces) {
      Process ps = null;
      try {
        ps = Runtime.getRuntime().exec("ifconfig -L " + ni);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()))) {
          String line;
          while ((line = br.readLine()) != null) {
            if (line.indexOf("inet") == -1) {
              continue;
            }
            String ip = line.split(" ")[1];
            return ip;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        try {
          ps.destroyForcibly();
        } catch (Exception e) {
        }
      }
    }
    return null;
  }

  @Test
  public void basicallyWorks() throws UnknownHostException {
    String validLocalHost = NetworkUtils.getLocalhost();
    // System.out.println(validLocalHost);
    InetAddress addr = InetAddress.getByName(validLocalHost);
    if (isNetworkUp()) {
      System.out.println("running network-up test");
      assertThat(validLocalHost).isNotEqualTo(NetworkUtils.NULL_SAFE_LOCALHOST);
      assertThat(validLocalHost).isEqualTo(NetworkUtils.getLocalhost(validLocalHost))
          .isNotEqualTo(NetworkUtils.NULL_SAFE_LOCALHOST);
      assertThat(NetworkUtils.isValidAddress(addr)).isTrue();
    } else {
      System.err.println("running network-down test");
      String nullSafeLocalHost = NetworkUtils.getLocalhost("127");
      assertThat(nullSafeLocalHost).isEqualTo(NetworkUtils.NULL_SAFE_LOCALHOST);
      assertThat(validLocalHost).isEqualTo(NetworkUtils.NULL_SAFE_LOCALHOST);
      assertThat(NetworkUtils.isValidAddress(addr)).isFalse();
      assertThat(NetworkUtils.isValidAddress(InetAddress.getByName(nullSafeLocalHost))).isFalse();
    }
    assertThat(NetworkUtils.isValidAddress(InetAddress.getByName(NetworkUtils.ANYHOST))).isFalse();

  }

  @Test(expected = UnknownHostException.class)
  public void basicallyWorks2() throws UnknownHostException {
    System.out.println(NetworkUtils.isValidAddress(InetAddress.getByName("aa")));
  }

  @Test
  public void basicallyWorks3() {
    String msg = "Only ip-segment checking is not supported yet";
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> NetworkUtils.getLocalhost(null, 1, 2))
        .withMessage(msg);
  }

  @Test
  public void basicallyWorks4() {
    System.out.println("running network-up test");
    String expectedIp = tryGetIpAnotherWay("en0", "en1");
    System.out.println("expectedIp: " + expectedIp);
    if (!isNetworkUp()) {
      assertThat(expectedIp).isNull();
      expectedIp = NetworkUtils.NULL_SAFE_LOCALHOST;
    }
    // just the filter is not matched,but we have a valid ip->
    try {
      NetworkUtils.getLocalhost("888");
    } catch (IpNotMatchException e) {
      assertThat(e.getMessage()).isEqualTo(Strings.lenientFormat(
          "Got unexpected local-ip-address, expect ip (start-with '888' and third-seg-range ignored), but the actual ip is '%s'",
          expectedIp));
    }
    // <-

    // below shows the ip-seg-range filter works
    // 1.the normal case
    String firstSeg = expectedIp.substring(0, expectedIp.indexOf(".") + 1);
    int thirdSeg = Integer.parseInt(expectedIp.split("\\.")[2]);
    System.out.println("firstSeg: " + firstSeg);
    System.out.println("thirdSeg: " + thirdSeg);
    String addrStr = NetworkUtils.getLocalhost(firstSeg, thirdSeg, 888, 999);// include thirdSeg
    assertThat(addrStr).isEqualTo(expectedIp);

    // 2.the abnormal case
    try {
      NetworkUtils.getLocalhost(firstSeg, 888, 999);// not include
    } catch (IpNotMatchException e) {
      assertThat(e.getMessage()).isEqualTo(Strings.lenientFormat(
          "Got unexpected local-ip-address, expect ip (start-with '%s' and third-seg-range within [888, 999]), but the actual ip is '%s'",
          firstSeg, expectedIp));
    }
  }
}
