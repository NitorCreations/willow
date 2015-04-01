package com.nitorcreations.willow.deployer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.AbstractModule;
import com.nitorcreations.willow.messages.WebSocketTransmitter;

public class DeployerModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(WebSocketTransmitter.class).toInstance(new WebSocketTransmitter());
    bind(ExecutorService.class).toInstance(Executors.newCachedThreadPool());
  }
}
