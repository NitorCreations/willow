package com.nitorcreations.willow.metrics;

import static com.nitorcreations.willow.metrics.MetricUtils.median;

import java.util.List;

import javax.inject.Named;

import com.nitorcreations.willow.messages.metrics.MetricConfig;

@Named("/latency")
public class RequestDurationMetric extends SimpleMetric<Long, Long> {
  @Override
  public String getType() {
    return "access";
  }

  @Override
  public String[] requiresFields() {
    return new String[] { "duration" };
  }

  @Override
  protected Long estimateValue(List<Long> preceeding, long stepTime, long stepLen, MetricConfig conf) {
    return median(preceeding);
  }

  @Override
  protected Long fillMissingValue() {
    return 0L;
  }
}
