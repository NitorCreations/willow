package com.nitorcreations.willow.metrics;

public class PhysicalMemoryMetric extends SimpleMetric<Double, Double> {
  @Override
  public String getType() {
    return "mem";
  }

  @Override
  public String[] requiresFields() {
    return new String[] { "usedPercent" };
  }
}
