package com.nitorcreations.willow.messages;

public class NetInterface extends AbstractMessage {
  public String name;
  public long rxBytes;
  public long rxPackets;
  public long rxErrors;
  public long rxDropped;
  public long rxOverruns;
  public long rxFrame;
  public long txBytes;
  public long txPackets;
  public long txErrors;
  public long txDropped;
  public long txOverruns;
  public long txCollisions;
  public long txCarrier;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getRxBytes() {
    return rxBytes;
  }

  public void setRxBytes(long rxBytes) {
    this.rxBytes = rxBytes;
  }

  public long getRxPackets() {
    return rxPackets;
  }

  public void setRxPackets(long rxPackets) {
    this.rxPackets = rxPackets;
  }

  public long getRxErrors() {
    return rxErrors;
  }

  public void setRxErrors(long rxErrors) {
    this.rxErrors = rxErrors;
  }

  public long getRxDropped() {
    return rxDropped;
  }

  public void setRxDropped(long rxDropped) {
    this.rxDropped = rxDropped;
  }

  public long getRxOverruns() {
    return rxOverruns;
  }

  public void setRxOverruns(long rxOverruns) {
    this.rxOverruns = rxOverruns;
  }

  public long getRxFrame() {
    return rxFrame;
  }

  public void setRxFrame(long rxFrame) {
    this.rxFrame = rxFrame;
  }

  public long getTxBytes() {
    return txBytes;
  }

  public void setTxBytes(long txBytes) {
    this.txBytes = txBytes;
  }

  public long getTxPackets() {
    return txPackets;
  }

  public void setTxPackets(long txPackets) {
    this.txPackets = txPackets;
  }

  public long getTxErrors() {
    return txErrors;
  }

  public void setTxErrors(long txErrors) {
    this.txErrors = txErrors;
  }

  public long getTxDropped() {
    return txDropped;
  }

  public void setTxDropped(long txDropped) {
    this.txDropped = txDropped;
  }

  public long getTxOverruns() {
    return txOverruns;
  }

  public void setTxOverruns(long txOverruns) {
    this.txOverruns = txOverruns;
  }

  public long getTxCollisions() {
    return txCollisions;
  }

  public void setTxCollisions(long txCollisions) {
    this.txCollisions = txCollisions;
  }

  public long getTxCarrier() {
    return txCarrier;
  }

  public void setTxCarrier(long txCarrier) {
    this.txCarrier = txCarrier;
  }
}
