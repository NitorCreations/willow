package com.nitorcreations.willow.messages;

/**
 * Message structure for sending stack trace element data in a thread dump.
 * 
 * @author Mikko Tommila
 */
public class StackTraceData {

  public StackTraceData() {
  }

  public StackTraceData(StackTraceElement stackTraceElement) {
    this.declaringClass = stackTraceElement.getClassName();
    this.methodName = stackTraceElement.getMethodName();
    this.fileName = stackTraceElement.getFileName();
    this.lineNumber = stackTraceElement.getLineNumber();
  }

  public String declaringClass;
  public String methodName;
  public String fileName;
  public int lineNumber;
}