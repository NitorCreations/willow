package com.nitorcreations.willow.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractStreamPumper implements Runnable {
  protected AtomicBoolean running = new AtomicBoolean(true);
  protected final BufferedReader in;
  private final String name;
  public AbstractStreamPumper(InputStream in, String name, Charset charset) {
    this.in = new BufferedReader(new InputStreamReader(in, charset));
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
      Logger.getAnonymousLogger().log(Level.INFO, "Stream pumping exception", e);
    }
  }

  public abstract void handle(String line);

  public String getName() {
    return name;
  }
}
