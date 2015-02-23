package com.nitorcreations.willow.metrics;

import javax.inject.Named;

@Named("/hosts")
public class HostTagMetric extends TagListMetric {
  public HostTagMetric() {
    super("host");
  }
}
