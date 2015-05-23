package com.nitorcreations.willow.autoscaler.scaling;

import com.google.inject.Inject;
import com.nitorcreations.willow.autoscaler.clouds.CloudAdapter;
import com.nitorcreations.willow.autoscaler.clouds.CloudAdapters;
import com.nitorcreations.willow.autoscaler.config.AutoScalingGroupConfig;
import com.nitorcreations.willow.autoscaler.config.AutoScalingPolicy;
import com.nitorcreations.willow.autoscaler.metrics.AutoScalingGroupStatus;
import com.nitorcreations.willow.autoscaler.metrics.AutoScalingStatus;
import com.nitorcreations.willow.messages.WebSocketTransmitter;
import com.nitorcreations.willow.messages.event.MetricThresholdClearedEvent;
import com.nitorcreations.willow.messages.event.MetricThresholdTriggeredEvent;
import com.nitorcreations.willow.messages.event.ScaleInEvent;
import com.nitorcreations.willow.messages.event.ScaleOutEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scaler implements Runnable {

  public static final String TRIGGERED_POLICY_HAS_NO_EFFECT_ON_RUNNING_INSTANCES = "Triggered policy has no effect on running instances";
  private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

  public AtomicBoolean running = new AtomicBoolean(true);

  @Inject
  ScheduledExecutorService scheduler;

  @Inject
  AutoScalingStatus autoScalingStatus;

  @Inject
  private CloudAdapters cloudAdapters;

  @Inject
  private WebSocketTransmitter transmitter;

  public void initialize(List<AutoScalingGroupConfig> groups) {
    for (final AutoScalingGroupConfig group : groups) {
      scheduler.scheduleAtFixedRate(new Runnable() {

        Set<AutoScalingPolicy> triggeredPolicyHistory = new HashSet<>();

        @Override
        public void run() {
          try {
            logger.info("Autoscaler checking triggered policies for group " + group.getName());
            AutoScalingGroupStatus groupStatus = autoScalingStatus.getStatus(group.getName());
            CloudAdapter cloud = cloudAdapters.get(group.getCloudProvider());

            actOnInstanceCountBounds(groupStatus, cloud);
            List<AutoScalingPolicy> triggeredPolicies = groupStatus.getTriggeredPolicies();
            maintainPolicyTriggerHistory(groupStatus, triggeredPolicies);
            actOnPolicies(groupStatus, cloud, triggeredPolicies);
          } catch (Exception e) {
            logger.log(Level.SEVERE, "Scaler failure", e);
          }
        }

        private void actOnPolicies(AutoScalingGroupStatus groupStatus, CloudAdapter cloud, List<AutoScalingPolicy> triggeredPolicies) {
          if (!triggeredPolicies.isEmpty()) {
            AutoScalingPolicy policy = triggeredPolicies.get(0); //TODO priority order?
            logger.info(String.format("Plan action for triggered policy %s for %s group %s", policy.getName(), group.getCloudProvider(), group.getName()));
            int currentInstances = groupStatus.getDeploymentStatus().getInstanceCount();
            int effect = policy.getPolicyEffect(currentInstances);
            if (effect > 0) {
              if (currentInstances + effect > group.getInstanceMaxCount()) {
                logger.info(String.format("Capping scale out effect to maximum of %s instances", group.getInstanceMaxCount()));
                effect = group.getInstanceMaxCount() - currentInstances;
              }
              if (effect > 0) {
                scaleOut(group, effect, cloud, policy);
              } else {
                noEffect();
              }
            } else if (effect < 0) {
              if (currentInstances + effect < group.getInstanceBaseCount()) {
                effect = currentInstances - group.getInstanceBaseCount();
                logger.info(String.format("Capping scale in effect to keep minimum of %s instances", group.getInstanceBaseCount()));
              }
              effect = Math.abs(effect);
              if (effect > 0) {
                scaleIn(group, effect, cloud, policy);
              } else {
                noEffect();
              }
            } else {
              noEffect();
            }
          }
        }

        private void noEffect() {
          logger.info(TRIGGERED_POLICY_HAS_NO_EFFECT_ON_RUNNING_INSTANCES);
        }

        private void maintainPolicyTriggerHistory(AutoScalingGroupStatus groupStatus, List<AutoScalingPolicy> triggeredPolicies) {
          for (AutoScalingPolicy policy : triggeredPolicies) {
            if (!triggeredPolicyHistory.contains(policy)) {
              logger.info(String.format("Triggered policy %s for %s group %s", policy.getName(), group.getCloudProvider(), group.getName()));
              sendMetricThresholdTriggeredEvent(policy, group, groupStatus);
              triggeredPolicyHistory.add(policy);
            }
          }
          Set<AutoScalingPolicy> clearedPolicies = new HashSet(triggeredPolicyHistory);
          clearedPolicies.removeAll(triggeredPolicies);
          for (AutoScalingPolicy policy : clearedPolicies) {
            logger.info(String.format("Policy %s for %s group %s no longer in triggered state", policy.getName(), group.getCloudProvider(), group.getName()));
            sendMetricThresholdClearedEvent(policy, group, groupStatus);
          }
          triggeredPolicyHistory.retainAll(triggeredPolicies);
        }

        private void actOnInstanceCountBounds(AutoScalingGroupStatus groupStatus, CloudAdapter cloud) {
          if (groupStatus.getDeploymentStatus() != null) {
            int currentCount = groupStatus.getDeploymentStatus().getInstanceCount();
            if (currentCount < group.getInstanceBaseCount()) {
              int startCount =group.getInstanceBaseCount() - currentCount;
              logger.info(String.format("%s group %s has %s instances running which is less than minimum of %s. Starting %s instances",
                  group.getCloudProvider(), group.getName(), currentCount, group.getInstanceBaseCount(), startCount));
              scaleOut(group, startCount, cloud, null);
            } else if (currentCount > group.getInstanceMaxCount()) {
              int terminateCount = currentCount - group.getInstanceMaxCount();
              logger.info(String.format("%s group %s has %s instances running which is more than maximum of %s. Terminating %s instances",
                  group.getCloudProvider(), group.getName(), currentCount, group.getInstanceBaseCount(), terminateCount));
              scaleIn(group, terminateCount, cloud, null);
            }
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

        private void scaleOut(AutoScalingGroupConfig group, int effect, CloudAdapter cloud, AutoScalingPolicy policy) {
          logger.info(String.format("Starting %s instances in %s group %s", effect, group.getCloudProvider(), group.getName()));
          List<String> instanceIds = cloud.launchInstances(group, effect);
          sendScaleOutEvent(policy, group, instanceIds);
          quietPeriod();
        }

        private void scaleIn(AutoScalingGroupConfig group, int effect, CloudAdapter cloud, AutoScalingPolicy policy) {
          logger.info(String.format("Terminating %s instances in %s group %s", effect, group.getCloudProvider(), group.getName()));
          List<String> instanceIds = cloud.terminateInstances(group, effect);
          sendScaleInEvent(policy, group, instanceIds);
          quietPeriod();
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

  private void sendMetricThresholdTriggeredEvent(AutoScalingPolicy policy, AutoScalingGroupConfig groupConfig,
                                                 AutoScalingGroupStatus groupStatus) {
    MetricThresholdTriggeredEvent mte = new MetricThresholdTriggeredEvent();
    mte.metric = policy.getMetricName();
    mte.threshold = policy.getMetricThreshold().doubleValue();
    mte.value = groupStatus.getLastValueFor(policy.getMetricName()).getValue();
    mte.addTag("group_" + groupConfig.getName());
    mte.description = String.format(
        "Metric %s value %s is past threshold of %s defined in scaling policy %s. Policy action: %s",
        mte.metric,
        mte.value,
        mte.threshold,
        policy.getName(),
        policy.getScalingAction()
    );
    transmitter.queue(mte);
  }

  private void sendMetricThresholdClearedEvent(AutoScalingPolicy policy, AutoScalingGroupConfig groupConfig, AutoScalingGroupStatus groupStatus) {
    MetricThresholdClearedEvent mte = new MetricThresholdClearedEvent();
    mte.metric = policy.getMetricName();
    mte.threshold = policy.getMetricThreshold().doubleValue();
    mte.value = groupStatus.getLastValueFor(policy.getMetricName()).getValue();
    mte.addTag("group_" + groupConfig.getName());
    mte.description = String.format(
        "Metric %s value %s is back within threshold of %s defined in scaling policy %s.",
        mte.metric,
        mte.value,
        mte.threshold,
        policy.getName()
    );
    transmitter.queue(mte);
  }

  private void sendScaleInEvent(AutoScalingPolicy policy, AutoScalingGroupConfig groupConfig,
                                List<String> instanceIds)  {
    ScaleInEvent sie = new ScaleInEvent();
    sie.addTag("group_" + groupConfig.getName());
    sie.cloudProvider = groupConfig.getCloudProvider();
    sie.group = groupConfig.getName();
    sie.instanceCount = instanceIds.size();
    sie.instanceIds = instanceIds;
    sie.policy = policy != null ? policy.getName() : "maxCount";
    sie.description = String.format(
        "Terminated %s instances in %s group %s due to policy %s",
        sie.instanceCount,
        sie.cloudProvider,
        sie.group,
        sie.policy
    );
    transmitter.queue(sie);
  }

  private void sendScaleOutEvent(AutoScalingPolicy policy, AutoScalingGroupConfig groupConfig,
                                 List<String> instanceIds)  {
    ScaleOutEvent soe = new ScaleOutEvent();
    soe.addTag("group_" + groupConfig.getName());
    soe.cloudProvider = groupConfig.getCloudProvider();
    soe.group = groupConfig.getName();
    soe.instanceCount = instanceIds.size();
    soe.instanceIds = instanceIds;
    soe.policy = policy != null ? policy.getName() : "baseCount";
    soe.description = String.format(
        "Launched %s new instances in %s group %s due to policy %s",
        soe.instanceCount,
        soe.cloudProvider,
        soe.group,
        soe.policy
    );
    transmitter.queue(soe);
  }
}
