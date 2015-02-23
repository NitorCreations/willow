package com.nitorcreations.willow.metrics;

import javax.inject.Named;

@Named("/types")
public class MessageTypesMetric extends TagListMetric {
  public MessageTypesMetric() {
    super("category");
  }
}
