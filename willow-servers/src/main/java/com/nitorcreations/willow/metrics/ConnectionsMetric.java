package com.nitorcreations.willow.metrics;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

@Named("/tcpinfo")
public class ConnectionsMetric extends SimpleMetric<Integer, Integer> {
  @Override
  public String getType() {
    return "tcpinfo";
  }

  @Override
  public String[] requiresFields() {
    return new String[] { "tcpInboundTotal", "tcpOutboundTotal" };
  }

  @Override
  protected Integer getValue(List<Integer> arr) {
    return (arr.get(0).intValue() + arr.get(1).intValue());
  }

  @Override
  protected Integer estimateValue(List<Integer> preceeding, long stepTime, long stepLen, MetricConfig conf) {
    return Collections.max(preceeding);
  }
}
