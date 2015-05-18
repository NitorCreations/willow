package com.nitorcreations.willow.utils;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingStreamPumper extends AbstractStreamPumper implements Runnable {
  private Logger log;
  private Level level;

  public LoggingStreamPumper(InputStream in, Level level, String name, Charset charset) {
    super(in, name, charset);
    this.level = level;
    this.log = Logger.getLogger(name);
  }

  @Override
  public void handle(String line) {
    log.log(level, line);
  }
}
