package com.nitorcreations.willow.messages;

import java.lang.management.LockInfo;

/**
 * Message structure for sending thread locking data in a thread dump.
 * 
 * @author Mikko Tommila
 */
public class LockData {

  public LockData() {
  }

  public LockData(LockInfo lockInfo) {
    this.className = lockInfo.getClassName();
    this.identityHashCode = lockInfo.getIdentityHashCode();
  }

  public String className;
  public int identityHashCode;
}