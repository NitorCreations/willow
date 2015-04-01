package com.nitorcreations.willow.deployer;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractStatisticsSender implements StatisticsSender {
  protected AtomicBoolean running = new AtomicBoolean(true);
  
  @Override
  public final void run() {
    while (running.get() && !Thread.currentThread().isInterrupted()) {
      execute();
    }
  }

  public void stop() {
    running.set(false);
  }
  public abstract void execute();
}
