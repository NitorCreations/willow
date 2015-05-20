package com.nitorcreations.willow.messages.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * An event to be triggered when a deployer child process finishes restarting.
 */
@SuppressFBWarnings(value={"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"}, justification="Fields used in serialization")
public class ChildRestartedEvent extends Event{

  public final String deployerName;
  public final Long pid;

  public ChildRestartedEvent(String deployerName, Long pid) {
    this.deployerName = deployerName;
    this.pid = pid;
    this.description = String.format("Child process for deployer %s restarted with pid %s", deployerName, pid);
  }
}
