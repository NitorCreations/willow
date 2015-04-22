package com.nitorcreations.willow.messages;

import org.msgpack.annotation.Message;

@Message
public class Processes extends AbstractMessage {
  public long total;
  public long idle;
  public long running;
  public long sleeping;
  public long stopped;
  public long zombie;
  public long threads;

  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }

  public long getIdle() {
    return idle;
  }

  public void setIdle(long idle) {
    this.idle = idle;
  }

  public long getRunning() {
    return running;
  }

  public void setRunning(long running) {
    this.running = running;
  }

  public long getSleeping() {
    return sleeping;
  }

  public void setSleeping(long sleeping) {
    this.sleeping = sleeping;
  }

  public long getStopped() {
    return stopped;
  }

  public void setStopped(long stopped) {
    this.stopped = stopped;
  }

  public long getZombie() {
    return zombie;
  }

  public void setZombie(long zombie) {
    this.zombie = zombie;
  }

  public long getThreads() {
    return threads;
  }

  public void setThreads(long threads) {
    this.threads = threads;
  }
}
