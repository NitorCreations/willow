package com.nitorcreations.willow.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class HostUtil {
	public static String getHostName() {
		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface nic = interfaces.nextElement();
				Enumeration<InetAddress> addresses = nic.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					if (!address.isLoopbackAddress()) {
						return address.getHostName();
					}
				}
			}
			return "localhost";
		} catch (SocketException e) {
			return "localhost";
		}
	}

}
