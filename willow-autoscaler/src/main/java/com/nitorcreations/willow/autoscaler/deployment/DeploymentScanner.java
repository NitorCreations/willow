package com.nitorcreations.willow.autoscaler.deployment;

import com.nitorcreations.willow.autoscaler.clouds.CloudAdapter;
import com.nitorcreations.willow.autoscaler.clouds.CloudAdapters;
import com.nitorcreations.willow.autoscaler.config.AutoScalingGroupConfig;
import com.nitorcreations.willow.autoscaler.metrics.AutoScalingStatus;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
public class DeploymentScanner implements Runnable {

  private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

  @Inject
  private CloudAdapters cloudAdapters;

  @Inject
  private ScheduledExecutorService scheduledExecutorService;

  private List<AutoScalingGroupConfig> groups = new ArrayList<>();

  private Map<String, AutoScalingGroupDeploymentStatus> statuses = new ConcurrentHashMap<>();

  @Inject
  private AutoScalingStatus autoScalingStatus;

  private AtomicBoolean running = new AtomicBoolean(true);

  public void initialize(List<AutoScalingGroupConfig> groups) {
    if (groups == null) throw new IllegalArgumentException("List of groups can't be null");
    logger.info(String.format("Initializing deployment scanner with %s groups", groups.size()));
    this.setGroups(groups);
  }

  public AutoScalingGroupDeploymentStatus getStatus(String groupId) {
    return statuses.get(groupId);
  }

  public void stop() {
    running.set(false);
    scheduledExecutorService.shutdownNow();
  }

  public List<AutoScalingGroupConfig> getGroups() {
    ArrayList<AutoScalingGroupConfig> ret = new ArrayList<>();
    ret.addAll(groups);
    return ret;
  }

  public void setGroups(List<AutoScalingGroupConfig> groups) {
    this.groups.clear();
    this.groups.addAll(groups);
  }

  @Override
  public void run() {
    while (running.get()) {
      Map<ScheduledFuture<?>, AutoScalingGroupConfig> futures = new ConcurrentHashMap<>();
      for (final AutoScalingGroupConfig group : groups) {
        futures.put(scheduleGroupScan(group), group);
      }
      for (Map.Entry<ScheduledFuture<?>, AutoScalingGroupConfig> entry : futures.entrySet()) {
        ScheduledFuture<?> f = entry.getKey();
        try {
          f.get();
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          logger.log(Level.SEVERE, "deployment scanner execution failure", e);
          futures.put(scheduleGroupScan(futures.get(f)), futures.get(f));
          futures.remove(entry.getKey());
        }
      }
    }
    logger.info("DeploymentScanner exiting");
  }

  private ScheduledFuture<?> scheduleGroupScan(final AutoScalingGroupConfig group) {
    return scheduledExecutorService.scheduleAtFixedRate(
        new Runnable() {
          @Override
          public void run() {
            CloudAdapter cloud = cloudAdapters.get(group.getCloudProvider());
            AutoScalingGroupDeploymentStatus groupStatus = cloud.getGroupStatus(group.getRegion(), group.getName());
            logger.info(String.format("Deployment status for %s group %s: %s instances", group.getCloudProvider(),
                group.getName(), groupStatus.getInstanceCount()));
            statuses.put(group.getName(), groupStatus); //TODO remove internal structure if unnecessary
            autoScalingStatus.setDeploymentStatus(group.getName(), groupStatus);
          }
        }, 1, 10, TimeUnit.SECONDS);
  }
}
