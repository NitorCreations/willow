package com.nitorcreations.willow.messages;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

/**
 * Message structure for sending the data of one thread in a thread dump.
 * 
 * @author Mikko Tommila
 */
public class ThreadData {

  public ThreadData() {
  }

  public ThreadData(ThreadInfo threadInfo) {
    this.threadName = threadInfo.getThreadName();
    this.threadId = threadInfo.getThreadId();
    this.blockedTime = threadInfo.getBlockedTime();
    this.blockedCount = threadInfo.getBlockedCount();
    this.waitedTime = threadInfo.getWaitedTime();
    this.waitedCount = threadInfo.getWaitedCount();
    LockInfo lockInfo = threadInfo.getLockInfo();
    if (lockInfo != null) {
      this.lock = new LockData(lockInfo);
    }
    this.lockName = threadInfo.getLockName();
    this.lockOwnerId = threadInfo.getLockOwnerId();
    this.lockOwnerName = threadInfo.getLockOwnerName();
    this.inNative = threadInfo.isInNative();
    this.suspended = threadInfo.isSuspended();
    this.threadState = threadInfo.getThreadState().toString();

    StackTraceElement[] stackTraceData = threadInfo.getStackTrace();
    this.stackTrace = new StackTraceData[stackTraceData.length];
    for (int i = 0; i < stackTraceData.length; i++) {
      this.stackTrace[i] = new StackTraceData(stackTraceData[i]);
    }

    MonitorInfo[] lockedMonitorsData = threadInfo.getLockedMonitors();
    this.lockedMonitors = new MonitorData[lockedMonitorsData.length];
    for (int i = 0; i < lockedMonitorsData.length; i++) {
      this.lockedMonitors[i] = new MonitorData(lockedMonitorsData[i]);
    }

    LockInfo[] lockedSynchronizersData = threadInfo.getLockedSynchronizers();
    this.lockedSynchronizers = new LockData[lockedSynchronizersData.length];
    for (int i = 0; i < lockedSynchronizersData.length; i++) {
      this.lockedSynchronizers[i] = new LockData(lockedSynchronizersData[i]);
    }
  }

  public String threadName;
  public long threadId;
  public long blockedTime;
  public long blockedCount;
  public long waitedTime;
  public long waitedCount;
  public LockData lock;
  public String lockName;
  public long lockOwnerId;
  public String lockOwnerName;
  public boolean inNative;
  public boolean suspended;
  public String threadState;
  public StackTraceData[] stackTrace;
  public MonitorData[] lockedMonitors;
  public LockData[] lockedSynchronizers;
}