package com.nitorcreations.willow.metrics;

public class NetData {
	public NetData(String name, long rx, long tx) {
		this.name = name;
		this.rx = rx;
		this.tx = tx;
	}
	String name;
	long rx;
	long tx;
}
