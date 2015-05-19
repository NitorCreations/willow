package com.nitorcreations.willow.autoscaler.config;

import com.nitorcreations.willow.utils.MergeableProperties;

public class AutoScalingPolicy {

  private String name;
  private String metricName;
  private Integer metricThreshold;
  private String metricComparison;
  private String scalingAction;

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

  public void setScalingAction(String scalingAction) {
    this.scalingAction = scalingAction;
  }
}
