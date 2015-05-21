package com.nitorcreations.willow.autoscaler.scaling;

import com.google.inject.Inject;
import com.nitorcreations.willow.autoscaler.clouds.CloudAdapter;
import com.nitorcreations.willow.autoscaler.clouds.CloudAdapters;
import com.nitorcreations.willow.autoscaler.config.AutoScalingGroupConfig;
import com.nitorcreations.willow.autoscaler.config.AutoScalingPolicy;
import com.nitorcreations.willow.autoscaler.metrics.AutoScalingGroupStatus;
import com.nitorcreations.willow.autoscaler.metrics.AutoScalingStatus;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scaler implements Runnable {

  private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

  public AtomicBoolean running = new AtomicBoolean(true);

  @Inject
  ScheduledExecutorService scheduler;

  @Inject
  AutoScalingStatus autoScalingStatus;

  @Inject
  private CloudAdapters cloudAdapters;

  public void initialize(List<AutoScalingGroupConfig> groups) {
    for (final AutoScalingGroupConfig group : groups) {
      scheduler.scheduleAtFixedRate(new Runnable() {
        @Override
        public void run() {
          try {

            logger.info("autoscaler checking triggered policies for group " + group.getName());
            AutoScalingGroupStatus groupStatus = autoScalingStatus.getStatus(group.getName());
            CloudAdapter cloud = cloudAdapters.get(group.getCloudProvider());

            if (groupStatus.getDeploymentStatus() != null) {
              int currentCount = groupStatus.getDeploymentStatus().getInstanceCount();
              if (currentCount < group.getInstanceBaseCount()) {
                int startCount =group.getInstanceBaseCount() - currentCount;
                logger.info(String.format("%s group %s has %s instances running which is less than minimum of %s. Starting %s instances",
                    group.getCloudProvider(), group.getName(), currentCount, group.getInstanceBaseCount(), startCount));
                cloud.launchInstances(group, startCount);
                quietPeriod();
              } else if (currentCount > group.getInstanceMaxCount()) {
                int terminateCount = currentCount - group.getInstanceMaxCount();
                logger.info(String.format("%s group %s has %s instances running which is more than maximum of %s. Terminating %s instances",
                    group.getCloudProvider(), group.getName(), currentCount, group.getInstanceBaseCount(), terminateCount));
                cloud.terminateInstances(group, terminateCount);
                quietPeriod();
              }
            }

            List<AutoScalingPolicy> triggeredPolicies = groupStatus.getTriggeredPolicies();
            if (triggeredPolicies != null && !triggeredPolicies.isEmpty()) {
              AutoScalingPolicy policy = triggeredPolicies.get(0);
              logger.info(String.format("Triggered policy %s for %s group %s", policy.getName(), group.getCloudProvider(), group.getName()));
              int currentInstances = groupStatus.getDeploymentStatus().getInstanceCount();
              int effect = policy.getPolicyEffect(currentInstances);
              if (effect > 0) {
                if (currentInstances + effect > group.getInstanceMaxCount()) {
                  logger.info(String.format("Capping scale out effect to maximum of %s instances", group.getInstanceMaxCount()));
                  effect = group.getInstanceMaxCount() - currentInstances;
                }
                if (effect > 0) {
                  logger.info(String.format("Starting %s instances in %s group %s", effect, group.getCloudProvider(), group.getName()));
                  cloud.launchInstances(group, effect);
                  quietPeriod();
                }
              } else if (effect < 0) {
                if (currentInstances + effect < group.getInstanceBaseCount()) {
                  effect = currentInstances - group.getInstanceBaseCount();
                  logger.info(String.format("Sizing scale in effect to keep minimum of %s instances", group.getInstanceBaseCount()));
                }
                effect = Math.abs(effect);
                if (effect > 0) {
                  logger.info(String.format("Terminating %s instances in %s group %s", effect, group.getCloudProvider(), group.getName()));
                  cloud.terminateInstances(group, effect);
                  quietPeriod();
                }
              } else {
                logger.info("Triggered policy has no effect on running instances");
              }
            }
          } catch (Exception e) {
            logger.log(Level.SEVERE, "Scaler failure", e);
          }
        }

        private void quietPeriod() {
          try {
            logger.info(String.format("Entering %s second quiet period for %s group %s", group.getQuietPeriodSeconds(),
                group.getCloudProvider(), group.getName()));
            Thread.sleep(TimeUnit.SECONDS.toMillis(group.getQuietPeriodSeconds()));
            logger.info(String.format("Continuing after quiet period for %s group %s", group.getCloudProvider(), group.getName()));
          } catch (InterruptedException e) {
            logger.info(String.format("interrupted during quiet period for %s group %s", group.getCloudProvider(), group.getName()));
          }
        }

      }, 1, 10, TimeUnit.SECONDS);
    }
  }

  @Override
  public void run() {

  }

  public void stop() {
    this.running.set(false);
  }


}
