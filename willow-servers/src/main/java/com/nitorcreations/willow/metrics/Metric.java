package com.nitorcreations.willow.metrics;

import com.nitorcreations.willow.messages.metrics.MetricConfig;

public interface Metric {
  Object calculateMetric(MetricConfig conf);
  boolean hasData(MetricConfig conf);
}
