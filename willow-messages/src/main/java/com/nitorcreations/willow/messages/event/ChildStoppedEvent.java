package com.nitorcreations.willow.messages.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value={"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"}, justification="Fields used in serialization")
public class ChildStoppedEvent extends Event {

  public final String deployerName;
  public final long pid;
  public final int returnValue;
  public final boolean restart;

  public ChildStoppedEvent(String deployerName, long pid, int returnValue, boolean restart) {
    this.deployerName = deployerName;
    this.pid = pid;
    this.returnValue = returnValue;
    this.restart = restart;
    if (restart) {
      this.description = String.format("Child of deployer %s (pid %s) stopped with return value %s as part of a restart cycle", deployerName, pid, returnValue);
    } else {
      this.description = String.format("Child of deployer %s (pid %s) stopped with return value %s", deployerName, pid, returnValue);
    }
  }
}
