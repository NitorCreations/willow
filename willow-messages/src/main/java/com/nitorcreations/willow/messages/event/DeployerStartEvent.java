package com.nitorcreations.willow.messages.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * An event for willow-deployer startup.
 */
@SuppressFBWarnings(value={"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"}, justification="Fields used in serialization")
public class DeployerStartEvent extends Event {

  public final long pid;
  public final String deployerName;

  public DeployerStartEvent(long pid, String deployerName) {
    this.pid = pid;
    this.deployerName = deployerName;
    this.description = String.format("Deployer %s started.", deployerName);
  }

}
