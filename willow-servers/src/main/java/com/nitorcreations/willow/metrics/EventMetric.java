package com.nitorcreations.willow.metrics;

import java.util.Collection;

import javax.inject.Named;

import com.nitorcreations.willow.messages.event.EventMessage;

@Named("/event")
public class EventMetric extends FullMessageMetric<EventMessage,Collection<EventMessage>> {

  @Override
  protected Collection<EventMessage> processData(long start, long stop, int step, MetricConfig conf) {
    return rawData.values();
  }
}
