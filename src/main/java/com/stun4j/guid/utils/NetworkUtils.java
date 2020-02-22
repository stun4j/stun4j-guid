package com.stun4j.guid.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A very simple network util
 * <p>
 * get best available local ip-address<br>
 * be able to pick ip by specifing ip-prefix
 * 
 * @author Jay Meng
 */
public final class NetworkUtils {
  private static final Logger LOG = LoggerFactory.getLogger(NetworkUtils.class);
  private static volatile InetAddress LOCAL_ADDRESS = null;
  public static final String LOCALHOST = "127.0.0.1";
  public static final String ANYHOST = "0.0.0.0";
  private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

  public static String getLocalHost() {
    return getLocalHost(null);
  }

  public static String getLocalHost(String ipStartith) {
    InetAddress address = getLocalAddress(ipStartith);
    return address == null ? LOCALHOST : address.getHostAddress();
  }

  public static InetAddress getLocalAddress() {
    return getLocalAddress(null);
  }

  public static InetAddress getLocalAddress(String ipStartwith) {
    if (LOCAL_ADDRESS != null)
      return LOCAL_ADDRESS;
    InetAddress localAddress = getLocalAddress0(ipStartwith);
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

  private static boolean isValidAddress(InetAddress inetAddr) {
    if (inetAddr == null || inetAddr.isLoopbackAddress())
      return false;
    String name = inetAddr.getHostAddress();
    return (name != null && !ANYHOST.equals(name) && !LOCALHOST.equals(name) && IP_PATTERN.matcher(name).matches());
  }

  private NetworkUtils() {
  }

}
