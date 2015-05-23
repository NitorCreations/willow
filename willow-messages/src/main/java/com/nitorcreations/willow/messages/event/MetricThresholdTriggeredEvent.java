package com.nitorcreations.willow.messages.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * An event for triggering a metric value threshold, e.g. CPU load rises above an upper bound
 * threshold or descends below a lower bound threshold.
 */
@SuppressFBWarnings(value={"UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD"}, justification="Fields used in serialization")
public class MetricThresholdTriggeredEvent extends Event {

  public String metric;
  public Double value;
  public Double threshold;

}
