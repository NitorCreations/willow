package com.nitorcreations.willow.autoscaler.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutoScalingGroupDeploymentStatus {

  private final String name;
  private final List<String> instances;

  public AutoScalingGroupDeploymentStatus(String name, List<String> instances) {
    this.name = name;
    this.instances = Collections.unmodifiableList(new ArrayList<>(instances));
  }

  public String getName() {
    return name;
  }

  public List<String> getInstances() {
    return instances;
  }

  public int getInstanceCount() {
    return instances.size();
  }

}
