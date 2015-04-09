package com.nitorcreations.willow.messages;

import java.lang.management.ThreadInfo;

/**
 * Message structure for sending a thread dump.
 * 
 * @author Mikko Tommila
 */
public class ThreadDumpMessage extends AbstractMessage {

  public ThreadDumpMessage() {
  }

  public ThreadDumpMessage(ThreadInfo[] threadInfo) {
    this.threadData = new ThreadData[threadInfo.length];
    for (int i = 0; i < threadInfo.length; i++) {
      this.threadData[i] = new ThreadData(threadInfo[i]);
    }
  }

  public ThreadData[] threadData;
}
