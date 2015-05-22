package com.nitorcreations.willow.autoscaler.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutoScalingGroupDeploymentStatus {

  private final String name;
  private final List<Instance> instances;

  public AutoScalingGroupDeploymentStatus(String name, List<Instance> instances) {
    this.name = name;
    this.instances = Collections.unmodifiableList(new ArrayList<>(instances));
  }

  public String getName() {
    return name;
  }

  public List<Instance> getInstances() {
    return Collections.unmodifiableList(instances);
  }

  public int getInstanceCount() {
    return instances.size();
  }

}
