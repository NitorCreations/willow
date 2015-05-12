package com.nitorcreations.willow.metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Named;

@Named("/net")
public class NetworkMetric extends SimpleMetric<NetData, Object> {
  private HashMap<String, NetData> prevValues = new HashMap<>();

  @Override
  public String getType() {
    return "net";
  }

  @Override
  public String[] requiresFields() {
    return new String[] { "name", "txBytes", "rxBytes" };
  }

  @Override
  protected NetData getValue(List<Object> results) {
    NetData ret = new NetData((String) results.get(0), ((Number) results.get(1)).longValue(), ((Number) results.get(2)).longValue());
    if (!prevValues.containsKey(ret.name))
      prevValues.put(ret.name, ret);
    return ret;
  }

  @Override
  protected Double estimateValue(List<NetData> preceeding, long stepTime, long stepLen, MetricConfig conf) {
    HashMap<String, NetData> lasts = new HashMap<>();
    for (NetData next : preceeding) {
      lasts.put(next.name, next);
    }
    long netBytes = 0;
    for (Entry<String, NetData> next : lasts.entrySet()) {
      NetData start = prevValues.get(next.getKey());
      if (start == null)
        continue;
      netBytes += (next.getValue().rx - start.rx);
      netBytes += (next.getValue().tx - start.tx);
    }
    prevValues.putAll(lasts);
    return (1000 * netBytes) / (double) (1024 * stepLen);
  }

  @Override
  protected Double fillMissingValue() {
    return 0D;
  }
}
