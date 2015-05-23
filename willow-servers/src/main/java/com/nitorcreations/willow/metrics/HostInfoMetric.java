package com.nitorcreations.willow.metrics;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import com.nitorcreations.willow.messages.HostInfoMessage;
import com.nitorcreations.willow.messages.metrics.MetricConfig;

@Named("/hostinfo")
public class HostInfoMetric extends FullMessageMetric<HostInfoMessage, Collection<HostInfoMessage>> {

  @Override
  protected Collection<HostInfoMessage> processData(long start, long stop, int step, MetricConfig conf) {
    Map<String, HostInfoMessage> map = new HashMap<>();
    for (HostInfoMessage him : rawData.values()) {
      map.put(him.getInstance(), him);
    }
    return map.values();
  }

}

