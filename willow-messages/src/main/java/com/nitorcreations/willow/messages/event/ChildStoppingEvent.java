package com.nitorcreations.willow.messages.event;

/**
 * An event to be triggered when a deployer child process enters the stopping state.
 */
public class ChildStoppingEvent extends Event {

  public final String deployerName;
  public final long pid;

  public ChildStoppingEvent(String deployerName, long pid) {
    this.deployerName = deployerName;
    this.pid = pid;
    this.description = String.format("Deployer %s stopping child process with pid %s", deployerName, pid);
  }
}
