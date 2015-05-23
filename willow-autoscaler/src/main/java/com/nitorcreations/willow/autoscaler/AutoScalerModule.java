package com.nitorcreations.willow.autoscaler;

import com.google.inject.AbstractModule;
import com.nitorcreations.willow.messages.WebSocketTransmitter;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class AutoScalerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ScheduledExecutorService.class).toInstance(Executors.newScheduledThreadPool(3));
    bind(ExecutorService.class).toInstance(Executors.newCachedThreadPool());
    bind(Random.class);
    bind(WebSocketTransmitter.class).toInstance(new WebSocketTransmitter());
  }
}
