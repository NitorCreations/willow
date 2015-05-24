package com.nitorcreations.willow.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.client.Client;

import com.nitorcreations.willow.messages.metrics.MetricConfig;

@Named("/available")
public class AvailableMetricsMetric implements Metric {
  private final Logger log = Logger.getLogger(getClass().getCanonicalName());
  @Inject
  private transient Map<String, Metric> metrics;

  @Override
  public Object calculateMetric(Client client, MetricConfig conf) {
    HashMap<String, Boolean> ret = new HashMap<String, Boolean>();
    for (Entry<String, Metric> next : metrics.entrySet()) {
      Class<? extends Metric> nextClass = next.getValue().getClass();
      if (nextClass != this.getClass()) {
        try {
          Metric m = nextClass.newInstance();
          ret.put(next.getKey(), m.hasData(client, conf));
        } catch (InstantiationException | IllegalAccessException e) {
          log.log(Level.FINE, "Failed to create metric", e);
        }
      }
    }
    return ret;
  }

  @Override
  public boolean hasData(Client client, MetricConfig conf) {
    return true;
  }


}
