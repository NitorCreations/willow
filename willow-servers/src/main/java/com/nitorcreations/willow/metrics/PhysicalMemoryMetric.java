package com.nitorcreations.willow.metrics;

import javax.inject.Named;

@Named("/mem")
public class PhysicalMemoryMetric extends SimpleMetric<Double, Double> {
  @Override
  public String getType() {
    return "mem";
  }

  @Override
  public String[] requiresFields() {
    return new String[] { "usedPercent" };
  }

  @Override
  protected Double fillMissingValue() {
    return 0D;
  }
}
