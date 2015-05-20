package com.nitorcreations.willow.messages.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * An event to be triggered when a deployer child process enters the starting state.
 */
@SuppressFBWarnings(value={"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"}, justification="Fields used in serialization")
public class ChildStartingEvent extends Event {

  public final String deployerName;

  public ChildStartingEvent(String deployerName) {
    this.deployerName = deployerName;
    this.description = String.format("Deployer %s starting child process", deployerName);
  }
}
