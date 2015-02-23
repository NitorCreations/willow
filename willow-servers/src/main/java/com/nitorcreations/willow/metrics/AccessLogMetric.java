package com.nitorcreations.willow.metrics;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.client.Client;

import com.nitorcreations.willow.messages.AccessLogEntry;

@Named("/access")
public class AccessLogMetric extends FullMessageMetric<AccessLogEntry, Long, Long> {
  long[] limits = new long[] { 10L, 100L, 1000L, 10000L };

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
  public Collection<SeriesData<Long, Long>> calculateMetric(Client client, HttpServletRequest req) {
    String[] limitStrs = req.getParameterValues("limit");
    if (limitStrs != null && limitStrs.length > 0) {
      limits = new long[limitStrs.length];
      for (int i = 0; i < limitStrs.length; i++) {
        limits[i] = Long.parseLong(limitStrs[i]);
      }
    }
    if ("statuses".equals(req.getParameter("type"))) {
      getter = new ValueGetter() {
        @Override
        public long getValue(AccessLogEntry next) {
          return next.getStatus();
        }
      };
      if (limitStrs == null) {
        limits = new long[] { 200L, 300L, 400L, 500L, 600L };
      }
    }
    return super.calculateMetric(client, req);
  }

  @Override
  protected void addValue(Map<String, SeriesData<Long, Long>> values, List<AccessLogEntry> preceeding, long stepTime, long stepLen) {
    long[] buckets = new long[limits.length + 1];
    for (AccessLogEntry next : preceeding) {
      boolean inLimits = false;
      for (int i = 0; i < limits.length; i++) {
        if (next.getDuration() < limits[i]) {
          buckets[i]++;
          inLimits = true;
          break;
        }
      }
      if (!inLimits)
        buckets[limits.length]++;
    }
    String lower = "";
    addBucket(values, lower + "-", stepTime, buckets[0]);
    for (int i = 0; i < limits.length; i++) {
      lower = Long.toString(limits[i]);
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
