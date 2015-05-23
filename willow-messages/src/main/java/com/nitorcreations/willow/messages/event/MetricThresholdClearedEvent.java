package com.nitorcreations.willow.messages.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * An event for a metric threshold trigger returning to normal, non-triggered state.
 */
@SuppressFBWarnings(value={"UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD"}, justification="Fields used in serialization")
public class MetricThresholdClearedEvent extends Event {

  public String metric;
  public Double value;
  public Double threshold;

}
