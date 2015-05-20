package com.nitorcreations.willow.messages.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * An event to be triggered when a deployer child process enters the stopping state.
 */
@SuppressFBWarnings(value={"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"}, justification="Fields used in serialization")
public class ChildStoppingEvent extends Event {

  public final String deployerName;
  public final long pid;

  public ChildStoppingEvent(String deployerName, long pid) {
    this.deployerName = deployerName;
    this.pid = pid;
    this.description = String.format("Deployer %s stopping child process with pid %s", deployerName, pid);
  }
}
