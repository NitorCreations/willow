package com.nitorcreations.willow.messages.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value={"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"}, justification="Fields used in serialization")
public class ChildDiedEvent extends Event {

  public final String deployerName;
  public final long pid;
  public final int returnValue;

  public ChildDiedEvent(String deployerName, long pid, int returnValue) {
    this.deployerName = deployerName;
    this.pid = pid;
    this.returnValue = returnValue;
    this.description = String.format("Child of deployer %s (pid %s) exited unexpectedly with return value %s", deployerName, pid, returnValue);
  }
}
