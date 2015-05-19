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
    policy.name = (String)properties.get("name");
    policy.metricName = (String)properties.get("metricName");
    policy.metricThreshold = Integer.valueOf((String)properties.get("metricThreshold"));
    policy.metricComparison = (String)properties.get("metricComparison");
    policy.scalingAction = (String)properties.get("scalingAction");
    return policy;
  }
}
