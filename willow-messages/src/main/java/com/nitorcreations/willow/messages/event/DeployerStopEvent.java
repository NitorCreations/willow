package com.nitorcreations.willow.messages.event;

/**
 * An event for willow-deployer stop.
 */
public class DeployerStopEvent extends Event {

  public final long pid;
  public final String deployerName;

  public DeployerStopEvent(long pid, String deployerName) {
    this.pid = pid;
    this.deployerName = deployerName;
    this.description = String.format("Deployer %s stopped.", deployerName);
  }

}
