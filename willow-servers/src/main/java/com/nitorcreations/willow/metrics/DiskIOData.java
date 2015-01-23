package com.nitorcreations.willow.metrics;

public class DiskIOData {
  public DiskIOData(String device, long read, long write) {
    this.device = device;
    this.read = read;
    this.write = write;
  }
  String device;
  long read;
  long write;
}
