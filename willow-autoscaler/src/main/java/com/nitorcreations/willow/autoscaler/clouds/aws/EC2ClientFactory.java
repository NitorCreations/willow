package com.nitorcreations.willow.autoscaler.clouds.aws;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class EC2ClientFactory {

  public synchronized AmazonEC2Client getClient(String regionName) {
    return Region.getRegion(Regions.fromName(regionName)).createClient(AmazonEC2Client.class, null, null);
  }

}
