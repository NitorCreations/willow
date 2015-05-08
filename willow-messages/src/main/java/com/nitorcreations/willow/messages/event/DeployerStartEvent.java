package com.nitorcreations.willow.messages.event;

/**
 * An event for willow-deployer startup.
 */
public class DeployerStartEvent extends Event {

  public final long pid;
  public final String deployerName;

  public DeployerStartEvent(long pid, String deployerName) {
    this.pid = pid;
    this.deployerName = deployerName;
    this.description = String.format("Deployer %s started.", deployerName);
  }

}
