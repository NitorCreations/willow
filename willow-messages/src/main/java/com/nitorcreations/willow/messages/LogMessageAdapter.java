package com.nitorcreations.willow.messages;

public interface LogMessageAdapter {
  long getLogEntryTimeStamp();
  String getLevel();
  String getMessage();
  String getThread();
  String getLogger();
  String getStackTrace();
}
