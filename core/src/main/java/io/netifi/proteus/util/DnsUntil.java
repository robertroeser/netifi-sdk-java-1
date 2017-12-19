package io.netifi.proteus.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import static io.netifi.proteus.util.StringUtil.hasLetters;

public final class DnsUntil {
  private DnsUntil() {}

  public static SocketAddress checkInetSocketAddress(SocketAddress address) {
    InetSocketAddress inetSocketAddress = (InetSocketAddress) address;
    String hostName = inetSocketAddress.getHostName();
    if (hasLetters(hostName)) {
      return toIpAddress(hostName, inetSocketAddress.getPort());
    } else {
      return inetSocketAddress;
    }
  }

  public static InetSocketAddress toIpAddress(String hostName, int port) {
    InetSocketAddress socketAddress;
    try {
      InetAddress address = InetAddress.getByName(hostName);
      String ipAddress = address.getHostAddress();
      socketAddress = InetSocketAddress.createUnresolved(ipAddress, port);
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }

    return socketAddress;
  }
}
