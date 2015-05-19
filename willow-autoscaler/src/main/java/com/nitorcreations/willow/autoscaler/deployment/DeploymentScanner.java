package com.nitorcreations.willow.autoscaler.deployment;

import com.google.inject.Inject;
import com.nitorcreations.willow.autoscaler.clouds.CloudAdapter;
import com.nitorcreations.willow.autoscaler.clouds.CloudAdapters;
import com.nitorcreations.willow.autoscaler.config.AutoScalingGroupConfig;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Named
public class DeploymentScanner {

  @Inject
  private CloudAdapters cloudAdapters;

  private ScheduledExecutorService scheduledExecutorService;

  private List<AutoScalingGroupConfig> groups = new ArrayList<>();

  private Map<String, AutoScalingGroupStatus> statuses = new ConcurrentHashMap<>();

  public void initialize(List<AutoScalingGroupConfig> groups) {
    this.groups = groups;
    statuses = new ConcurrentHashMap<>();
    if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
      scheduledExecutorService.shutdownNow();
    }
    scheduledExecutorService = Executors.newScheduledThreadPool(groups.size());
    for (final AutoScalingGroupConfig group : groups) {
      scheduledExecutorService.scheduleAtFixedRate(
          new Runnable() {
            @Override
            public void run() {
              System.out.println("running");
              CloudAdapter cloud = cloudAdapters.get(group.getCloudProvider());
              AutoScalingGroupStatus groupStatus = cloud.getGroupStatus(group.getRegion(), group.getName());
              statuses.put(group.getName(), groupStatus);
            }
          }, 1, 10, TimeUnit.SECONDS);
    }
  }

  public AutoScalingGroupStatus getStatus(String groupId) {
    return statuses.get(groupId);
  }

  public void stop() {
    scheduledExecutorService.shutdownNow();
  }

}

