package com.nitorcreations.willow.metrics;

class CpuData {
  public CpuData(long idle, long total) {
    this.idle = idle;
    this.total = total;
  }
  long idle;
  long total;
}
