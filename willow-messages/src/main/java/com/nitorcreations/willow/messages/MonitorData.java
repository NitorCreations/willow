package com.nitorcreations.willow.messages;

import java.lang.management.MonitorInfo;

/**
 * Message structure for sending thread monitor data in a thread dump.
 * 
 * @author Mikko Tommila
 */
public class MonitorData extends LockData {

  public MonitorData() {
  }

  public MonitorData(MonitorInfo monitorInfo) {
    super(monitorInfo);
    this.stackDepth = monitorInfo.getLockedStackDepth();
    StackTraceElement lockedStackFrame = monitorInfo.getLockedStackFrame();
    if (lockedStackFrame != null) {
      this.stackFrame = new StackTraceData(lockedStackFrame);
    }
  }

  public int stackDepth;
  public StackTraceData stackFrame;
}