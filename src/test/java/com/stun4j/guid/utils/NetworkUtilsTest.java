package com.stun4j.guid.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

import com.stun4j.guid.exception.IpNotMatchException;

public class NetworkUtilsTest {
  // Not applicable to Intranet environments (Intranet that cannot access the Internet)
  static boolean isNetworkUp() {
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
    if (!isNetworkUp()) {
      System.err.println("network down,test canceled");
      return;
    }
    System.out.println("running network-up test");
    String expectedIp = tryGetIpAnotherWay("en0", "en1");
    try {
      NetworkUtils.getLocalhost("888");
    } catch (IpNotMatchException e) {
      assertThat(e.getMessage()).isEqualTo(Strings.lenientFormat(
          "Got unexpected local-ip-address, expect ip (start-with '888' and third-seg-range ignored), but the actual ip is '%s'",
          expectedIp));
    }
    System.out.println("expectedIp: " + expectedIp);
    String firstSeg = expectedIp.substring(0, expectedIp.indexOf(".") + 1);
    int thirdSeg = Integer.parseInt(expectedIp.split("\\.")[2]);
    System.out.println("firstSeg: " + firstSeg);
    System.out.println("thirdSeg: " + thirdSeg);
    String addrStr = NetworkUtils.getLocalhost(firstSeg, thirdSeg, 888, 999);
    assertThat(addrStr).isEqualTo(expectedIp);
    // <-

    try {
      NetworkUtils.getLocalhost(firstSeg, 888, 999);
    } catch (IpNotMatchException e) {
      assertThat(e.getMessage()).isEqualTo(Strings.lenientFormat(
          "Got unexpected local-ip-address, expect ip (start-with '%s' and third-seg-range within [888, 999]), but the actual ip is '%s'",
          firstSeg, expectedIp));
    }
  }
}
