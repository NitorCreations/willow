package com.nitorcreations.willow.messages.event;

/**
 * An event for passing a metric value threshold
 */
public class MetricThresholdEvent extends Event {

  public String metric;
  public Double value;
  public Double threshold;

  public MetricThresholdEvent() {
    super();
  }

  public MetricThresholdEvent(EventMessage eventMessage) {
    super();
    description = eventMessage.description;
    metric = eventMessage.eventData.get("metric");
    value = Double.valueOf(eventMessage.eventData.get("value"));
    threshold = Double.valueOf(eventMessage.eventData.get("threshold"));
  }

}
