package com.nitorcreations.willow.autoscaler.clouds.aws;

import javax.inject.Named;
import javax.inject.Singleton;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;

@Named
@Singleton
public class EC2ClientFactory {

  public synchronized AmazonEC2Client getClient(String regionName) {
    Region region = Region.getRegion(Regions.fromName(regionName));
    if (region != null) {
      return region.createClient(AmazonEC2Client.class, null, null);
    } else {
      throw new RuntimeException("Unable to create client for region " + regionName);
    }
  }

}
