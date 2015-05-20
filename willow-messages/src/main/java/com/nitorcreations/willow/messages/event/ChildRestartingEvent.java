package com.nitorcreations.willow.messages.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * An event to be triggered when a deployer child process enters the restarting state.
 */
@SuppressFBWarnings(value={"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"}, justification="Fields used in serialization")
public class ChildRestartingEvent extends Event {

  public final String deployerName;

  public ChildRestartingEvent(String deployerName) {
    this.deployerName = deployerName;
    this.description = String.format("Deployer %s restarting child process", deployerName);
  }
}
