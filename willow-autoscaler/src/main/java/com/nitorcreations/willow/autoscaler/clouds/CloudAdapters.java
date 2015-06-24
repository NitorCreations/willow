package com.nitorcreations.willow.autoscaler.clouds;

import java.util.Map;

import javax.inject.Named;

import com.google.inject.Inject;

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
