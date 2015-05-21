package com.nitorcreations.willow.autoscaler.config;

import com.nitorcreations.willow.utils.MergeableProperties;

public class AutoScalingPolicy {

  private String name;
  private String metricName;
  private Integer metricThreshold;
  private String metricComparison;
  private String scalingAction;

  private Double scalingMultiplier = null;
  private Integer scalingStaticValue = null;

  public static AutoScalingPolicy fromProperties(MergeableProperties properties) {
    AutoScalingPolicy policy = new AutoScalingPolicy();
    policy.setName((String)properties.get("name"));
    policy.setMetricName((String)properties.get("metricName"));
    policy.setMetricThreshold(Integer.valueOf((String)properties.get("metricThreshold")));
    policy.setMetricComparison((String)properties.get("metricComparison"));
    policy.setScalingAction((String)properties.get("scalingAction"));
    return policy;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMetricName() {
    return metricName;
  }

  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  public Integer getMetricThreshold() {
    return metricThreshold;
  }

  public void setMetricThreshold(Integer metricThreshold) {
    this.metricThreshold = metricThreshold;
  }

  public String getMetricComparison() {
    return metricComparison;
  }

  public void setMetricComparison(String metricComparison) {
    this.metricComparison = metricComparison;
  }

  public String getScalingAction() {
    return scalingAction;
  }

  public void setScalingAction(String input) {
    this.scalingAction = input;
    if (input != null && (input.startsWith("-") || input.startsWith("+"))) {
      if (input.endsWith("%")) {
        scalingMultiplier = Double.valueOf(input.substring(0, input.length()-1));
        scalingMultiplier = scalingMultiplier / 100D;
      } else {
        scalingStaticValue = Integer.valueOf(input);
      }
    }
  }

  public int getPolicyEffect(int currentInstanceCount) {
    int effect = 0;
    if (scalingMultiplier != null) {
      effect = (int) Math.round(currentInstanceCount * scalingMultiplier);
      //
      if (effect == 0 && scalingMultiplier > 0) {
        effect = 1;
      } else if (effect == 0 && scalingMultiplier < 0) {
        effect = -1;
      }

    } else {
      effect = scalingStaticValue;
    }
    //make sure we don't return an effect that takes instance count below zero.
    while (currentInstanceCount + effect < 0) {
      ++effect;
    }
    return effect;
  }
}
