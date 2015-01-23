package com.nitorcreations.willow.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractStreamPumper implements Runnable {
  protected AtomicBoolean running = new AtomicBoolean(true);
  protected final BufferedReader in;
  protected final String name;

  public AbstractStreamPumper(InputStream in, String name) {
    this.in = new BufferedReader(new InputStreamReader(in));
    this.name = name;
  }

  public void stop() {
    running.set(false);
  }

  @Override
  public void run() {
    try {
      String line;
      while (running.get() && (line = in.readLine()) != null) {
        if (!line.isEmpty()) {
          handle(line);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public abstract void handle(String line);
}
