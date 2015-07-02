package com.nitorcreations.willow.eventhandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.AbstractModule;

/**
 * Guice module for injecting event handler dependencies.
 * 
 * @author mtommila
 */
public class EventHandlerModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(ExecutorService.class).toInstance(Executors.newCachedThreadPool());
    }
  }
