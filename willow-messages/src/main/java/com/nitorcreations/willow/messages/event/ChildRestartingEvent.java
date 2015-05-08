package com.nitorcreations.willow.messages.event;

/**
 * An event to be triggered when a deployer child process enters the restarting state.
 */
public class ChildRestartingEvent extends Event {

  public final String deployerName;

  public ChildRestartingEvent(String deployerName) {
    this.deployerName = deployerName;
    this.description = String.format("Deployer %s restarting child process", deployerName);
  }
}
