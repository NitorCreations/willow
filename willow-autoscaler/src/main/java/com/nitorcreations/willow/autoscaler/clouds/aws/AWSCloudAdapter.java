package com.nitorcreations.willow.autoscaler.clouds.aws;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.nitorcreations.willow.autoscaler.deployment.AutoScalingGroupStatus;
import com.nitorcreations.willow.autoscaler.clouds.CloudAdapter;
import com.nitorcreations.willow.autoscaler.config.AutoScalingGroupConfig;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.LinkedList;
import java.util.List;

@Named("AWS")
public class AWSCloudAdapter implements CloudAdapter {

  @Inject
  EC2ClientFactory ec2ClientFactory;

  @Override
  public String getCloudProviderId() {
    return "AWS";
  }

  @Override
  public AutoScalingGroupStatus getGroupStatus(String regionId, String groupId) {
    List<String> instanceIds = new LinkedList<>();
    AmazonEC2Client client = ec2ClientFactory.getClient(regionId);
    DescribeInstancesRequest request = new DescribeInstancesRequest();
    request.withFilters(
        new Filter()
            .withName("tag:willow-group")
            .withValues(groupId)
    );
    DescribeInstancesResult result = client.describeInstances(request);
    for (Reservation r : result.getReservations()) {
      for (Instance instance : r.getInstances()) {
        instanceIds.add(instance.getInstanceId());
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
    return new AutoScalingGroupStatus(groupId, instanceIds);
  }

  @Override
  public List<String> startInstances(AutoScalingGroupConfig config, int count) {
    return null;
  }

  @Override
  public boolean terminateInstances(AutoScalingGroupConfig config, int count) {
    return false;
  }

}
