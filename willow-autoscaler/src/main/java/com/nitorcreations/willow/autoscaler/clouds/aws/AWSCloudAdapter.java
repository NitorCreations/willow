package com.nitorcreations.willow.autoscaler.clouds.aws;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.nitorcreations.willow.autoscaler.clouds.CloudAdapter;
import com.nitorcreations.willow.autoscaler.config.AutoScalingGroupConfig;
import com.nitorcreations.willow.autoscaler.deployment.AutoScalingGroupDeploymentStatus;
import com.nitorcreations.willow.autoscaler.metrics.AutoScalingGroupStatus;
import com.nitorcreations.willow.autoscaler.metrics.AutoScalingStatus;
import org.apache.commons.codec.binary.Base64;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("AWS")
public class AWSCloudAdapter implements CloudAdapter {

  private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

  @Inject
  private EC2ClientFactory ec2ClientFactory;

  @Inject
  private AutoScalingStatus autoScalingStatus;

  @Inject
  private Random random;

  @Override
  public String getCloudProviderId() {
    return "AWS";
  }

  @Override
  public AutoScalingGroupDeploymentStatus getGroupStatus(String regionId, String groupId) {
    List<String> instanceIds = new LinkedList<>();
    AmazonEC2Client client = ec2ClientFactory.getClient(regionId);
    DescribeInstancesRequest request = new DescribeInstancesRequest();
    request.withFilters(
        new Filter()
            .withName("tag:willow-group")
            .withValues(groupId)
    );

    DescribeInstancesResult result;
    try {
      result = client.describeInstances(request);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to query AWS instance(s)", e);
      return null;
    }

    for (Reservation r : result.getReservations()) {
      for (Instance instance : r.getInstances()) {
        String state = instance.getState().getName();
        if ("running".equalsIgnoreCase(state) || "pending".equalsIgnoreCase(state)) {
          instanceIds.add(instance.getInstanceId());
        }
      }
    }
    while (result.getNextToken() != null) {
      DescribeInstancesRequest moreRequest = new DescribeInstancesRequest().withNextToken(result.getNextToken());
      result = client.describeInstances(moreRequest);
      for (Reservation r : result.getReservations()) {
        for (Instance instance : r.getInstances()) {
          instanceIds.add(instance.getInstanceId());
        }
      }
    }
    return new AutoScalingGroupDeploymentStatus(groupId, instanceIds);
  }

  @Override
  public List<String> launchInstances(AutoScalingGroupConfig config, int count) {
    List<String> instanceIds = new ArrayList<>();
    RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

    try {
      runInstancesRequest
          .withImageId(config.getVirtualMachineImage())
          .withInstanceType(config.getInstanceType())
          .withMinCount(count)
          .withMaxCount(count)
          .withKeyName(config.getSshKey())
          .withSecurityGroupIds(config.getSecurityGroups())
          .withSubnetId(config.getSubnet())
          .withUserData(Base64.encodeBase64String(config.getUserData().getBytes("UTF-8")))
          ;
    } catch (UnsupportedEncodingException e) {
      logger.log(Level.SEVERE, "UTF-8 not supported, all bets are off!", e);
    }

    logger.info("Sending runInstances request to AWS");
    AmazonEC2Client client = ec2ClientFactory.getClient(config.getRegion());
    RunInstancesResult result = null;
    try {
      result = client.runInstances(runInstancesRequest);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to launch AWS instance(s)", e);
      return instanceIds;
    }
    logger.info("Sent runInstances request to AWS");

    List<Instance> instances = result.getReservation().getInstances();
    for (Instance instance : instances) {
      CreateTagsRequest createTagsRequest = new CreateTagsRequest();
      createTagsRequest
          .withResources(instance.getInstanceId())
          .withTags(willowTagsToAWSTags(config.getTags()));
      logger.info("Tagging instance " + instance.getInstanceId());
      client.createTags(createTagsRequest);
      instanceIds.add(instance.getInstanceId());
    }
    return instanceIds;
  }

  @Override
  public boolean terminateInstances(AutoScalingGroupConfig config, int count) {
    List<String> idsToTerminate = chooseInstancesToTerminate(config, count);
    TerminateInstancesRequest request = new TerminateInstancesRequest();
    request.withInstanceIds(idsToTerminate);

    AmazonEC2Client client = ec2ClientFactory.getClient(config.getRegion());
    TerminateInstancesResult result;
    try {
      result = client.terminateInstances(request);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to terminate AWS instance(s)", e);
      return false;
    }

    return result.getTerminatingInstances().size() == count;
  }

  private List<String> chooseInstancesToTerminate(AutoScalingGroupConfig config, int count) {
    Set<String> idsToTerminate = new HashSet<>();
    AutoScalingGroupStatus groupStatus = autoScalingStatus.getStatus(config.getName());
    if (groupStatus != null && groupStatus.getDeploymentStatus() != null) {
      List<String> instanceIds = groupStatus.getDeploymentStatus().getInstances();
      assert instanceIds.size() >= count;
      while (idsToTerminate.size() < count) {
        int index = random.nextInt(instanceIds.size());
        String instanceId = instanceIds.get(index);
        idsToTerminate.add(instanceId);
        logger.info(String.format("Chose instance %s to terminate", instanceId));
      }
    }
    return new ArrayList<>(idsToTerminate);
  }


  private List<Tag> willowTagsToAWSTags(List<com.nitorcreations.willow.autoscaler.config.Tag> wTags) {
    List<Tag> tags = new ArrayList<>();
    for (com.nitorcreations.willow.autoscaler.config.Tag t : wTags) {
      tags.add(new Tag(t.name, t.value));
    }
    return tags;
  }
}
