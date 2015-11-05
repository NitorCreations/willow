package com.nitorcreations.willow.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class MavenFormatter extends Formatter {

  public String format(LogRecord record) {
    StringBuilder message = new StringBuilder();
    message.append("[").append(record.getLevel()).append("] ").append(record.getLoggerName()).append(": ").append(record.getMessage()).append("\n");
    if (record.getThrown() != null) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      record.getThrown().printStackTrace(pw);
      message.append(sw.getBuffer());
      message.append("\n");
    }
    return message.toString();
  }
}
