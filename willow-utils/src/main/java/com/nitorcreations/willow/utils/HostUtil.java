package com.nitorcreations.willow.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class HostUtil {
  public static String getHostName() {
    try {
      InetAddress ip = InetAddress.getLocalHost();
        return ip.getHostName();
    } catch (UnknownHostException e) {
        return "localhost";
    }
  }
}
