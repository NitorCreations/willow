package com.nitorcreations.willow.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HostUtil {

  private static Logger logger = Logger.getLogger(HostUtil.class.getName());

  public static String getHostName() {
    try {
      InetAddress ip = InetAddress.getLocalHost();
      return ip.getHostName();
    } catch (UnknownHostException e) {
      return "localhost";
    }
  }

  /**
   *
   * @return The first site local IP address encountered while looping through all
   *         network interfaces.
   */
  public static InetAddress getPrivateIpAddress() {
    try {
      Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
      while (ifaces.hasMoreElements()) {
        NetworkInterface ni = ifaces.nextElement();
        Enumeration<InetAddress> addresses = ni.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress address = addresses.nextElement();
          if (address.isSiteLocalAddress()) {
            return address;
          }
        }
      }
    } catch (SocketException e) {
      logger.log(Level.WARNING, "Unable to enumerate network interfaces", e);
    }
    return InetAddress.getLoopbackAddress();
  }

  /**
   *
   * @return The first non-local IP address encountered while looping through all
   *         network interfaces.
   */
  public static InetAddress getPublicIpAddress() {
    try {
      Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
      while (ifaces.hasMoreElements()) {
        NetworkInterface ni = ifaces.nextElement();
        Enumeration<InetAddress> addresses = ni.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress address = addresses.nextElement();
          if (isPublicAddress(address)) {
            return address;
          }
        }
      }
    } catch (SocketException e) {
      logger.log(Level.WARNING, "Unable to enumerate network interfaces", e);
    }
    return null;
  }

  private static boolean isPublicAddress(InetAddress address) {
    return !(address.isAnyLocalAddress() || address.isLinkLocalAddress() || address.isLoopbackAddress() || address.isSiteLocalAddress());
  }
}
