package com.nitorcreations.willow.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.apache.commons.lang3.time.FastDateFormat;

public class SimpleFormatter extends Formatter {
  @Override
  public String format(LogRecord record) {
    StringBuilder message = new StringBuilder();
    FastDateFormat format = FastDateFormat.getInstance("yyyy'/'MM'/'dd HH':'mm':'ss' ['", TimeZone.getDefault(), Locale.ENGLISH);
    message.append(format.format(record.getMillis())).append(record.getLevel()).append("] ").append(record.getLoggerName()).append("   ").append(record.getMessage()).append("\n");
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
