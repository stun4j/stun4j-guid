package com.stun4j.guid.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

public class NetworkUtilsTest {
  @Test
  public void basicallyWorks() throws UnknownHostException {
    String validLocalHost = NetworkUtils.getLocalHost();
    String nullSafeLocalHost = NetworkUtils.getLocalHost("123");
    System.out.println(validLocalHost);
    System.out.println(nullSafeLocalHost);
    assertThat(validLocalHost).isNotEqualTo(nullSafeLocalHost);
    assertThat(nullSafeLocalHost).isEqualTo(NetworkUtils.NULL_SAFE_LOCALHOST);

    validLocalHost = NetworkUtils.getLocalHost(null);
    assertThat(validLocalHost).isNotEqualTo(NetworkUtils.NULL_SAFE_LOCALHOST);

    InetAddress addr = InetAddress.getByName(validLocalHost);
    assertThat(NetworkUtils.isValidAddress(addr)).isTrue();
    assertThat(NetworkUtils.isValidAddress(InetAddress.getByName(nullSafeLocalHost))).isFalse();
    assertThat(NetworkUtils.isValidAddress(InetAddress.getByName(NetworkUtils.ANYHOST))).isFalse();
    
    validLocalHost = NetworkUtils.getLocalHost(validLocalHost);
    assertThat(validLocalHost).isEqualTo(validLocalHost).isNotEqualTo(NetworkUtils.NULL_SAFE_LOCALHOST);

  }

  @Test(expected = UnknownHostException.class)
  public void basicallyWorks2() throws UnknownHostException {
    System.out.println(NetworkUtils.isValidAddress(InetAddress.getByName("aa")));

  }
}
