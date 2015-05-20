package com.nitorcreations.willow.messages.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * An event for willow-deployer stop.
 */
@SuppressFBWarnings(value={"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"}, justification="Fields used in serialization")
public class DeployerStopEvent extends Event {

  public final long pid;
  public final String deployerName;

  public DeployerStopEvent(long pid, String deployerName) {
    this.pid = pid;
    this.deployerName = deployerName;
    this.description = String.format("Deployer %s stopped.", deployerName);
  }

}
