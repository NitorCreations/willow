package com.nitorcreations.willow.metrics;

import com.nitorcreations.willow.messages.event.EventMessage;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

@Named("/event")
public class EventMetric extends FullMessageMetric<EventMessage,Collection<EventMessage>> {

  @Override
  protected Collection<EventMessage> processData(long start, long stop, int step, HttpServletRequest req) {
    return rawData.values();
  }
}
