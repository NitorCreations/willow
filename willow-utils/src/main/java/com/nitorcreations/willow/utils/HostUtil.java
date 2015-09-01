package com.nitorcreations.willow.utils;

import org.apache.commons.lang3.StringUtils;

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
   * @return The first site local IPv4 address encountered while looping through all
   * network interfaces. Return the loopback address as the last resort.
   * @param ifName interface name to use or null
   */
  public static InetAddress getPrivateIpAddress(String ifName) {
    NetworkInterface ni = getInterfaceByNameOrFirst(ifName);
    if (ni != null) {
      Enumeration<InetAddress> addresses = ni.getInetAddresses();
      while (addresses.hasMoreElements()) {
        InetAddress address = addresses.nextElement();
        if (address.isSiteLocalAddress() && !address.getHostAddress().contains(":")) {
          return address;
        }
      }
    }
    return InetAddress.getLoopbackAddress();
  }
  /**
   * @return The first non-local IPv4 address encountered while looping through all
   * network interfaces.
   * @param ifName interface name to use or null
   */
  public static InetAddress getPublicIpAddress(String ifName) {
    NetworkInterface ni = getInterfaceByNameOrFirst(ifName);
    if (ni != null) {
        Enumeration<InetAddress> addresses = ni.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress address = addresses.nextElement();
          if (isPublicAddress(address) && !address.getHostAddress().contains(":")) {
            return address;
          }
        }
      }
    return null;
  }

  private static boolean isPublicAddress(InetAddress address) {
    return !(address.isAnyLocalAddress() || address.isLinkLocalAddress() || address.isLoopbackAddress() || address.isSiteLocalAddress());
  }

  /**
   * @return interface matching name <code>ifName</code> or first interface if <code>ifName</code> is null.
   * @param ifName
   */
  private static NetworkInterface getInterfaceByNameOrFirst(String ifName) {
    try {
      Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
      while (ifaces.hasMoreElements()) {
        NetworkInterface ni = ifaces.nextElement();
        logger.log(Level.FINEST, "network interface name: " + ni.getDisplayName() + "/" + ni.getName());
        if (StringUtils.isEmpty(ifName)) {
          logger.log(Level.FINEST, "return ni " + ni.getName());
          return ni;
        } else {
          if (ifName.equalsIgnoreCase(ni.getName()) || ifName.equalsIgnoreCase(ni.getDisplayName())) {
            logger.log(Level.FINEST, "return ni " + ni.getName() + " matching with ifName " + ifName);
            return ni;
          }
        }
      }
    } catch (SocketException e) {
      logger.log(Level.WARNING, "Unable to enumerate network interfaces", e);
    }
    logger.warning("Could not find a network interface matching name: " + ifName);
    return null;
  }
}
