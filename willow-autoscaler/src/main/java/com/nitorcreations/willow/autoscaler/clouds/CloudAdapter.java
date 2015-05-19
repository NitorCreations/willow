package com.nitorcreations.willow.autoscaler.clouds;

import com.nitorcreations.willow.autoscaler.deployment.AutoScalingGroupStatus;
import com.nitorcreations.willow.autoscaler.config.AutoScalingGroupConfig;

import java.util.List;

public interface CloudAdapter {

  /**
   * The cloud provider that this adapter instance is for.
   *
   * @return cloudProviderId String
   */
  public String getCloudProviderId();

  /**
   * Query the cloud provider for status of an auto scaling group.
   * @param groupId
   */
  AutoScalingGroupStatus getGroupStatus(String regionId, String groupId);

  /**
   * Starts instances for the specified auto scaling group.
   * @param config the group to start instances in
   * @param count the number of instances to start
   * @return List of instance identifiers for the started instances
   */
  List<String> startInstances(AutoScalingGroupConfig config, int count);

  boolean terminateInstances(AutoScalingGroupConfig config, int count);
}
