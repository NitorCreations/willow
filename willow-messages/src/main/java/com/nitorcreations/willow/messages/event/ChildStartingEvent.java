package com.nitorcreations.willow.messages.event;

/**
 * An event to be triggered when a deployer child process enters the starting state.
 */
public class ChildStartingEvent extends Event {

  public final String deployerName;

  public ChildStartingEvent(String deployerName) {
    this.deployerName = deployerName;
    this.description = String.format("Deployer %s starting child process", deployerName);
  }
}
