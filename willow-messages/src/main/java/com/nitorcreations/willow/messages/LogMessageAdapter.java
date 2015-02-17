package com.nitorcreations.willow.messages;

public interface LogMessageAdapter {
  public long getLogEntryTimeStamp();
  public String getLevel();
  public String getMessage();
  public String getThread();
  public String getLogger();
  public String getStackTrace();
}
