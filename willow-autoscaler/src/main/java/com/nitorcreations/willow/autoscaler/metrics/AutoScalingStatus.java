package com.nitorcreations.willow.autoscaler.metrics;

import com.nitorcreations.willow.autoscaler.config.AutoScalingGroupConfig;
import com.nitorcreations.willow.autoscaler.deployment.AutoScalingGroupDeploymentStatus;
import com.nitorcreations.willow.metrics.TimePoint;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Named
@Singleton
public class AutoScalingStatus {

  //private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

  private Map<String, AutoScalingGroupStatus> statuses = new ConcurrentHashMap<>();

  private List<AutoScalingGroupConfig> groupConfigs = new ArrayList<>();

  public synchronized void initialize(List<AutoScalingGroupConfig> groups) {
    this.groupConfigs = groups;
    for (AutoScalingGroupConfig group : groups) {
      this.statuses.put(group.getName(), new AutoScalingGroupStatus(group));
    }
  }

  public AutoScalingGroupStatus getStatus(String group) {
    return statuses.get(group);
  }

  public void setDeploymentStatus(String group, AutoScalingGroupDeploymentStatus deploymentStatus) {
    AutoScalingGroupStatus currentStatus = getGroupStatus(group);
    currentStatus.setDeploymentStatus(deploymentStatus);
  }

  public void addMetricValue(String group, String metric, TimePoint metricValue) {
    AutoScalingGroupStatus currentStatus = getGroupStatus(group);
    currentStatus.addMetricValue(metric, metricValue);
  }

  public List<AutoScalingGroupConfig> getGroupConfigs() {
    return new ArrayList<>(groupConfigs);
  }

  private AutoScalingGroupStatus getGroupStatus(String group) {
    AutoScalingGroupStatus currentStatus = statuses.get(group);
    if (currentStatus == null) {
      throw new IllegalArgumentException("Unknown auto scaling group " + group);
    }
    return currentStatus;
  }

}
