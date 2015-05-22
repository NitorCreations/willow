package com.nitorcreations.willow.messages.event;

/**
 * An event for passing a metric value threshold
 */
public class MetricThresholdEvent extends Event {

  public String metric;
  public Double value;
  public Double threshold;

}
