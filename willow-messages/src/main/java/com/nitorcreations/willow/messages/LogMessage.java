package com.nitorcreations.willow.messages;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.LogRecord;

import org.msgpack.annotation.Message;


@Message
public class LogMessage extends AbstractMessage {
  public long logEntryTimeStamp;
  public String thread;
  public String level;
  public String logger;
  public String message;
  public String stackTrace;

  public LogMessage() {}

  public LogMessage(long logEntryTimeStamp, String level, String message) {
    this.logEntryTimeStamp = logEntryTimeStamp;
    this.level = level;
    this.message = message;
    this.stackTrace = null;
  }

  public LogMessage(long logEntryTimeStamp, String level, String message, String stackTrace) {
    this.logEntryTimeStamp = logEntryTimeStamp;
    this.level = level;
    this.message = message;
    this.stackTrace = stackTrace;
  }

  public LogMessage(LogMessageAdapter entry) {
    this.logEntryTimeStamp = entry.getLogEntryTimeStamp();
    this.thread = entry.getThread();
    this.level = entry.getLevel();
    this.logger = entry.getLogger();
    this.message = entry.getMessage();
    this.stackTrace = entry.getStackTrace();
  }
  
  public LogMessage(LogRecord entry) {
    this.thread = Integer.toString(entry.getThreadID());
    this.level = entry.getLevel().toString();
    this.logger = entry.getLoggerName();
    this.message = entry.getMessage();
    final Throwable throwable = entry.getThrown();
    if (throwable != null) {
      StringWriter str = new StringWriter();
      throwable.printStackTrace(new PrintWriter(str));
      this.stackTrace = str.toString();
    }
  }

  public long getLogEntryTimeStamp() {
    return logEntryTimeStamp;
  }

  public void setLogEntryTimeStamp(long logEntryTimeStamp) {
    this.logEntryTimeStamp = logEntryTimeStamp;
  }

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getThread() {
    return thread;
  }

  public void setThread(String thread) {
    this.thread = thread;
  }

  public String getLogger() {
    return logger;
  }

  public void setLogger(String logger) {
    this.logger = logger;
  }
}
