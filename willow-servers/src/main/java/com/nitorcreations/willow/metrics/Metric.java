package com.nitorcreations.willow.metrics;

import org.elasticsearch.client.Client;

public interface Metric {
  Object calculateMetric(Client client, MetricConfig conf);
}
