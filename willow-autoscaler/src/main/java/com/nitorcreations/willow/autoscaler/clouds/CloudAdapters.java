package com.nitorcreations.willow.autoscaler.clouds;

import com.google.inject.Inject;

import javax.inject.Named;
import java.util.Map;

@Named
public class CloudAdapters {

  @Inject
  private Map<String, CloudAdapter> cloudAdapters;

  public CloudAdapter get(String cloudProviderId) {
    CloudAdapter cloud = cloudAdapters.get(cloudProviderId);
    if (cloud == null) {
      throw new IllegalArgumentException("Unknown cloud provider " + cloudProviderId);
    }
    return cloud;
  }
}
