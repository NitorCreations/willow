package com.nitorcreations.willow.metrics;

import com.nitorcreations.willow.messages.HostInfoMessage;

import javax.inject.Named;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Named("/hostinfo")
public class HostInfoMetric extends FullMessageMetric<HostInfoMessage, Collection<HostInfoMessage>> {

  @Override
  protected Collection<HostInfoMessage> processData(long start, long stop, int step, MetricConfig conf) {
    Map<String, HostInfoMessage> map = new HashMap<>();
    for (HostInfoMessage him : rawData.values()) {
      map.put(him.instance, him);
    }
    return map.values();
  }

}

