package com.nitorcreations.willow.messages;

import org.msgpack.annotation.Message;

@Message
public class ThreadInfoMessage {
  public String threadName;
  public long threadId;
  public long blockedTime;
  public long blockedCount;
  public long waitedTime;
  public long waitedCount;
  public String lockName;
  public long lockOwnerId;
  public String lockOwnerName;
  public boolean inNative;
  public boolean suspended;
  public Thread.State threadState;

  public String getThreadName() {
    return threadName;
  }

  public void setThreadName(String threadName) {
    this.threadName = threadName;
  }

  public long getThreadId() {
    return threadId;
  }

  public void setThreadId(long threadId) {
    this.threadId = threadId;
  }

  public long getBlockedTime() {
    return blockedTime;
  }

  public void setBlockedTime(long blockedTime) {
    this.blockedTime = blockedTime;
  }

  public long getBlockedCount() {
    return blockedCount;
  }

  public void setBlockedCount(long blockedCount) {
    this.blockedCount = blockedCount;
  }

  public long getWaitedTime() {
    return waitedTime;
  }

  public void setWaitedTime(long waitedTime) {
    this.waitedTime = waitedTime;
  }

  public long getWaitedCount() {
    return waitedCount;
  }

  public void setWaitedCount(long waitedCount) {
    this.waitedCount = waitedCount;
  }

  public String getLockName() {
    return lockName;
  }

  public void setLockName(String lockName) {
    this.lockName = lockName;
  }

  public long getLockOwnerId() {
    return lockOwnerId;
  }

  public void setLockOwnerId(long lockOwnerId) {
    this.lockOwnerId = lockOwnerId;
  }

  public String getLockOwnerName() {
    return lockOwnerName;
  }

  public void setLockOwnerName(String lockOwnerName) {
    this.lockOwnerName = lockOwnerName;
  }

  public boolean isInNative() {
    return inNative;
  }

  public void setInNative(boolean inNative) {
    this.inNative = inNative;
  }

  public boolean isSuspended() {
    return suspended;
  }

  public void setSuspended(boolean suspended) {
    this.suspended = suspended;
  }

  public Thread.State getThreadState() {
    return threadState;
  }

  public void setThreadState(Thread.State threadState) {
    this.threadState = threadState;
  }
}
