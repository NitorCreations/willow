package com.nitorcreations.willow.messages;

import org.msgpack.annotation.Message;

@Message
public class Memory extends AbstractMessage {
  public long total;
  public long ram;
  public long used;
  public long free;
  public long actualUsed;
  public long actualFree;
  public double usedPercent;
  public double freePercent;

  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }

  public long getRam() {
    return ram;
  }

  public void setRam(long ram) {
    this.ram = ram;
  }

  public long getUsed() {
    return used;
  }

  public void setUsed(long used) {
    this.used = used;
  }

  public long getFree() {
    return free;
  }

  public void setFree(long free) {
    this.free = free;
  }

  public long getActualUsed() {
    return actualUsed;
  }

  public void setActualUsed(long actualUsed) {
    this.actualUsed = actualUsed;
  }

  public long getActualFree() {
    return actualFree;
  }

  public void setActualFree(long actualFree) {
    this.actualFree = actualFree;
  }

  public double getUsedPercent() {
    return usedPercent;
  }

  public void setUsedPercent(double usedPercent) {
    this.usedPercent = usedPercent;
  }

  public double getFreePercent() {
    return freePercent;
  }

  public void setFreePercent(double freePercent) {
    this.freePercent = freePercent;
  }
}
