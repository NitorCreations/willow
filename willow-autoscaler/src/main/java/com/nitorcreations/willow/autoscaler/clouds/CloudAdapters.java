package com.nitorcreations.willow.autoscaler.clouds;

import com.google.inject.Inject;

import javax.inject.Named;
import java.util.List;

@Named
public class CloudAdapters {

  @Inject
  private List<CloudAdapter> cloudAdapters;

  public CloudAdapter get(String cloudProviderId) {
    for (CloudAdapter ca : cloudAdapters) {
      if (ca.getCloudProviderId().equals(cloudProviderId)) {
        return ca;
      }
    }
    System.out.println("no adapter found");
    throw new IllegalArgumentException("Unknown cloud provider " + cloudProviderId);
  }
}
