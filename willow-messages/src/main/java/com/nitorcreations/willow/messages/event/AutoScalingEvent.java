package com.nitorcreations.willow.messages.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * Base class for auto scaling events.
 */
@SuppressFBWarnings(value={"UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD"}, justification="Fields used in serialization")
public class AutoScalingEvent extends Event {

  public Integer instanceCount;
  public String group;
  public String cloudProvider;
  public List<String> instanceIds;
  public String policy;

}
