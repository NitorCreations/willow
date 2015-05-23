package com.nitorcreations.willow.metrics;

import java.util.List;

import javax.inject.Named;

import com.nitorcreations.willow.messages.metrics.MetricConfig;

@Named("/requests")
public class RequestCountMetric extends SimpleMetric<Integer, Long> {
  @Override
  public String getType() {
    return "access";
  }

  @Override
  public String[] requiresFields() {
    return new String[] { "duration" };
  }

  @Override
  protected Integer estimateValue(List<Integer> preceeding, long stepTime, long stepLen, MetricConfig conf) {
    return preceeding.size();
  }

  @Override
  protected Integer fillMissingValue() {
    return 0;
  }
}
