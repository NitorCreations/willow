package com.nitorcreations.willow.autoscaler.deployment;

import com.nitorcreations.willow.autoscaler.clouds.CloudAdapter;
import com.nitorcreations.willow.autoscaler.clouds.CloudAdapters;
import com.nitorcreations.willow.autoscaler.config.AutoScalingGroupConfig;
import com.nitorcreations.willow.autoscaler.metrics.AutoScalingStatus;
import com.nitorcreations.willow.messages.HostInfoMessage;
import com.nitorcreations.willow.messages.WebSocketTransmitter;

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

  @Inject
  WebSocketTransmitter messageTransmitter;

  private Map<AutoScalingGroupConfig, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

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
    while (running.get() && !Thread.interrupted()) {
      for (final AutoScalingGroupConfig group : groups) {
        if (!futures.containsKey(group)) {
          futures.put(group, scheduleGroupScan(group));
        }
      }
      for (Map.Entry<AutoScalingGroupConfig, ScheduledFuture<?>> entry : futures.entrySet()) {
        ScheduledFuture<?> f = entry.getValue();
        try {
          f.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          logger.log(Level.INFO, "Interrupted", e);
        } catch (ExecutionException e) {
          logger.log(Level.SEVERE, "deployment scanner execution failure", e);
          futures.remove(entry.getKey());
        } catch (TimeoutException e) {
          logger.log(Level.FINEST, "Timeout - retrying");
          continue; //The timeouts allow periodic checking to ensure scanners keep running.
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
            sendHostInfo(groupStatus, group);
          }

          public void sendHostInfo(AutoScalingGroupDeploymentStatus deploymentStatus, AutoScalingGroupConfig group) {
            for (Instance i : deploymentStatus.getInstances()) {
              HostInfoMessage msg = new HostInfoMessage();
              msg.privateHostname = i.getPrivateHostname();
              msg.privateIpAddress = i.getPrivateIp();
              msg.publicHostname = i.getPublicHostname();
              msg.publicIpAddress = i.getPublicIp();
              msg.username = group.getUsername();
              msg.setInstance(i.getInstanceId().replaceAll("-", "_"));
              msg.addTags("host_"+msg.getInstance(), "group_"+group.getName());
              if (!messageTransmitter.queue(msg)) {
                logger.warning("Unable to queue hostInfoMessage for sending!");
              }
            }
          }
        }, 1, 10, TimeUnit.SECONDS);
  }
}
