package com.nitorcreations.willow.autoscaler.metrics;

import com.nitorcreations.willow.autoscaler.config.AutoScalingGroupConfig;
import com.nitorcreations.willow.autoscaler.config.AutoScalingPolicy;
import com.nitorcreations.willow.autoscaler.deployment.AutoScalingGroupDeploymentStatus;
import com.nitorcreations.willow.messages.metrics.TimePoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AutoScalingGroupStatus {

  private final AutoScalingGroupConfig group;
  private AutoScalingGroupDeploymentStatus deploymentStatus;
  private final Map<String, List<TimePoint<Double>>> metricsData;

  public AutoScalingGroupStatus(AutoScalingGroupConfig group) {
    this.group = group;
    this.deploymentStatus = null;
    this.metricsData = new ConcurrentHashMap<>();
  }

  public synchronized void setDeploymentStatus(AutoScalingGroupDeploymentStatus deploymentStatus) {
    this.deploymentStatus = deploymentStatus;
  }

  public synchronized void addMetricValue(String metricName, TimePoint<Double> value) {
    List<TimePoint<Double>> data = metricsData.get(metricName);
    if (data == null) {
      data = new ArrayList<>();
      metricsData.put(metricName, data);
    }
    data.add(value);
    if (data.size() > 200) {
      data.remove(0);
    }
  }

  public synchronized List<TimePoint<Double>> getMetricValues(String metricName) {
    List<TimePoint<Double>> data = metricsData.get(metricName);
    if (data == null) {
      return Collections.emptyList();
    }
    return new ArrayList<>(data);
  }

  public AutoScalingGroupConfig getGroup() {
    return group;
  }

  public synchronized AutoScalingGroupDeploymentStatus getDeploymentStatus() {
    return deploymentStatus;
  }

  /**
   * Returns a list of auto scaling policies that have triggered, e.g. value is past threshold.
   *
   * Considers a policy triggered if there a few data points consistently past the threshold.
   *
   * @return
   */
  public List<AutoScalingPolicy> getTriggeredPolicies() {
    List<AutoScalingPolicy> triggeredPolicies = new ArrayList<>();
    for (AutoScalingPolicy policy : group.getScalingPolicies()) {
      List<TimePoint<Double>> metricValues = getMetricValues(policy.getMetricName());
      int triggerCount = 0;
      int triggerThreshold = 3;
      int inspectCount = 0;
      if (metricValues != null && metricValues.size() > 2) {
        List<TimePoint<Double>> reverse = new ArrayList<>(metricValues);
        Collections.reverse(reverse);
        for (TimePoint<Double> p : reverse) {
          ComparisonOperation op = ComparisonOperation.fromSymbol(policy.getMetricComparison());
          if (op.compare(p.getValue(), Double.valueOf(policy.getMetricThreshold()))) {
            triggerCount++;
          }
          inspectCount++;
          if (inspectCount >= 3) {
            break;
          }
        }
        if (triggerCount >= triggerThreshold) {
          triggeredPolicies.add(policy);
        }
      }
    }
    return triggeredPolicies;
  }

  public TimePoint getLastValueFor(String metric) {
    List<TimePoint> data = getMetricValues(metric);
    if (!data.isEmpty()) {
      return data.get(data.size() - 1);
    } else {
      return null;
    }
  }
}
