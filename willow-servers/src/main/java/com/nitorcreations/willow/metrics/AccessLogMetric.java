package com.nitorcreations.willow.metrics;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.elasticsearch.client.Client;

import com.nitorcreations.willow.messages.AccessLogEntry;

@Named("/access")
public class AccessLogMetric extends FullMessageMultiseriesMetric<AccessLogEntry, Long, Long> {
  long[] limitValues = new long[] { 10L, 100L, 1000L, 10000L };

  private interface ValueGetter {
    long getValue(AccessLogEntry next);
  }
  private ValueGetter getter = new ValueGetter() {
    @Override
    public long getValue(AccessLogEntry next) {
      return next.getDuration();
    }
  };

  @Override
  public Collection<SeriesData<Long, Long>> calculateMetric(Client client, MetricConfig conf) {
    limitValues = new long[conf.getLimits().length];
    for (int i = 0; i < conf.getLimits().length; i++) {
      limitValues[i] = Long.parseLong(conf.getLimits()[i]);
    }
    if (conf.hasType("statuses")) {
      getter = new ValueGetter() {
        @Override
        public long getValue(AccessLogEntry next) {
          return next.getStatus();
        }
      };
      if (conf.getLimits().length == 0) {
        limitValues = new long[] { 200L, 300L, 400L, 500L, 600L };
      }
    }
    return super.calculateMetric(client, conf);
  }

  @Override
  protected void addValue(Map<String, SeriesData<Long, Long>> values, List<AccessLogEntry> preceeding, long stepTime, int stepLen, MetricConfig conf) {
    long[] buckets = new long[limitValues.length + 1];
    for (AccessLogEntry next : preceeding) {
      boolean inLimits = false;
      for (int i = 0; i < limitValues.length; i++) {
        if (next.getDuration() < limitValues[i]) {
          buckets[i]++;
          inLimits = true;
          break;
        }
      }
      if (!inLimits)
        buckets[limitValues.length]++;
    }
    String lower = "";
    addBucket(values, lower + "-", stepTime, buckets[0]);
    for (int i = 0; i < limitValues.length; i++) {
      lower = Long.toString(limitValues[i]);
    }
    addBucket(values, lower + "-", stepTime, buckets[buckets.length - 1]);
  }

  private void addBucket(Map<String, SeriesData<Long, Long>> values, String label, long time, long value) {
    SeriesData<Long, Long> bucket = values.get(label);
    if (bucket == null) {
      bucket = new SeriesData<>();
      bucket.key = label;
      values.put(label, bucket);
    }
    Point<Long, Long> p = new Point<>();
    p.x = time;
    p.y = value;
    bucket.values.add(p);
  }
}
