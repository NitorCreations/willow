package com.nitorcreations.willow.messages;

public class DiskIO extends AbstractMessage {
	long reads;
	long writes;
	long readBytes; 
	long writeBytes;
	double queue;
	double serviceTime;
	String name;
	String device;
	
	public long getReads() {
		return reads;
	}
	public void setReads(long reads) {
		this.reads = reads;
	}
	public long getWrites() {
		return writes;
	}
	public void setWrites(long writes) {
		this.writes = writes;
	}
	public long getReadBytes() {
		return readBytes;
	}
	public void setReadBytes(long readBytes) {
		this.readBytes = readBytes;
	}
	public long getWriteBytes() {
		return writeBytes;
	}
	public void setWriteBytes(long writeBytes) {
		this.writeBytes = writeBytes;
	}
	public double getQueue() {
		return queue;
	}
	public void setQueue(double queue) {
		this.queue = queue;
	}
	public double getServiceTime() {
		return serviceTime;
	}
	public void setServiceTime(double serviceTime) {
		this.serviceTime = serviceTime;
	}
	public void setName(String dirName) {
		this.name = dirName;
	}
	public String getName() {
		return this.name;
	}
	public void setDevice(String device) {
		this.device = device;
	}
	public String getDevice() {
		return this.device;
	}
}
