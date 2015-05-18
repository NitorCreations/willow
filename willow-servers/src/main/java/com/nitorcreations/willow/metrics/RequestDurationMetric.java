package com.nitorcreations.willow.metrics;

import java.util.List;

import javax.inject.Named;

@Named("/latency")
public class RequestDurationMetric extends SimpleMetric<Number, Long> {
  @Override
  public String getType() {
    return "access";
  }

  @Override
  public String[] requiresFields() {
    return new String[] { "duration" };
  }

  @Override
  protected Number estimateValue(List<Number> preceeding, long stepTime, long stepLen, MetricConfig conf) {
    if (preceeding.size() == 0) return 0;
    long sum = 0;
    for (Object next : preceeding) {
      sum += ((Number) next).longValue();
    }
    return sum / Math.max(preceeding.size(), 1);
  }
}
