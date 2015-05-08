package com.nitorcreations.willow.messages.event;

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
