package com.nitorcreations.willow.messages.event;

/**
 * An event to be triggered when a deployer child process enters the started state.
 */
public class ChildStartedEvent extends Event{

  public final String deployerName;
  public final Long pid;

  public ChildStartedEvent(String deployerName, Long pid) {
    this.deployerName = deployerName;
    this.pid = pid;
    this.description = String.format("Child process for deployer %s started with pid %s", deployerName, pid);
  }
}
