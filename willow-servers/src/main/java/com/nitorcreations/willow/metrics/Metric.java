package com.nitorcreations.willow.metrics;

import org.elasticsearch.client.Client;

import com.nitorcreations.willow.messages.metrics.MetricConfig;

public interface Metric {
  Object calculateMetric(Client client, MetricConfig conf);
  boolean hasData(Client client, MetricConfig conf);
}
