package com.nitorcreations.willow.deployer.statistics;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.nitorcreations.willow.messages.WebSocketTransmitter;

public abstract class AbstractStatisticsSender implements StatisticsSender {
  protected Logger logger = Logger.getLogger(getClass().getName());
  protected AtomicBoolean running = new AtomicBoolean(true);

  @Inject
  protected WebSocketTransmitter transmitter;
  private Thread thread;

  @Override
  public final void run() {
    thread = Thread.currentThread();
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
    if (thread != null) {
      thread.interrupt();
    }
  }

  public abstract void execute();
}
