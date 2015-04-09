package com.nitorcreations.willow.deployer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractStatisticsSender implements StatisticsSender {
  private Logger logger = Logger.getLogger(getClass().getName());
  protected AtomicBoolean running = new AtomicBoolean(true);
  
  @Override
  public final void run() {
    while (running.get() && !Thread.currentThread().isInterrupted()) {
      try {
        execute();
      } catch (RuntimeException re) {
        logger.log(Level.WARNING, "Unhandled exception", re);
      }
    }
  }

  public void stop() {
    running.set(false);
  }

  public abstract void execute();
}
