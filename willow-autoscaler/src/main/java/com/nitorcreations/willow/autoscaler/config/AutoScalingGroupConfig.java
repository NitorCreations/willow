package com.nitorcreations.willow.autoscaler.config;

import com.nitorcreations.willow.utils.MergeableProperties;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class AutoScalingGroupConfig {

  private static Logger logger = Logger.getLogger(AutoScalingGroupConfig.class.getCanonicalName());

  private String name;
  private String region;
  private String cloudProvider;
  private String virtualMachineImage;
  private String sshKey;
  private String username;
  private String instanceType;
  private String userData;
  private String network;
  private String subnet;
  private String authorizationRole;
  private List<String> securityGroups = new ArrayList<>();
  private Integer instanceBaseCount;
  private Integer instanceMaxCount;
  private List<AutoScalingPolicy> scalingPolicies = new ArrayList<>();
  private List<Tag> tags = new ArrayList<>();
  private Integer quietPeriodSeconds;

  public static AutoScalingGroupConfig fromProperties(MergeableProperties properties) {
    AutoScalingGroupConfig config = new AutoScalingGroupConfig();
    config.name = (String)properties.get("name");
    config.region = (String)properties.get("region");
    config.cloudProvider = (String)properties.get("cloudProvider");
    config.virtualMachineImage = (String)properties.get("virtualMachineImage");
    config.sshKey = (String)properties.get("sshKey");
    config.username = (String)properties.get("username");
    config.instanceType = (String)properties.get("instanceType");
    config.userData = (String)properties.get("userData");
    config.network = (String)properties.get("network");
    config.subnet = (String)properties.get("subnet");
    config.instanceBaseCount = Integer.valueOf((String)properties.get("instanceBaseCount"));
    config.instanceMaxCount = Integer.valueOf((String)properties.get("instanceMaxCount"));
    config.quietPeriodSeconds = Integer.valueOf((String)properties.get("quietPeriodSeconds"));

    List<MergeableProperties> scalingProps = properties.getPrefixedList("scalingPolicies");
    config.scalingPolicies = new LinkedList<>();
    for (MergeableProperties p : scalingProps) {
      config.scalingPolicies.add(AutoScalingPolicy.fromProperties(p));
    }

    config.authorizationRole = (String)properties.get("authorizationRole");
    config.securityGroups = properties.getDelimitedAsList("securityGroups", ",");

    List<MergeableProperties> tagProps = properties.getPrefixedList("tags");
    for (MergeableProperties p : tagProps) {
      Set<String> names = p.stringPropertyNames();
      if (names.size() > 1) {
        logger.severe("Invalid tag in auto scaling group configuration: too many properties");
      } else if (names.isEmpty()) {
        logger.severe("Invalid tag in auto scaling group configuration: no properties");
      }
      String tagName = names.iterator().next();
      config.tags.add(new Tag(tagName, p.getProperty(tagName)));
    }

    return config;
  }


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getCloudProvider() {
    return cloudProvider;
  }

  public void setCloudProvider(String cloudProvider) {
    this.cloudProvider = cloudProvider;
  }

  public String getVirtualMachineImage() {
    return virtualMachineImage;
  }

  public void setVirtualMachineImage(String virtualMachineImage) {
    this.virtualMachineImage = virtualMachineImage;
  }

  public String getSshKey() {
    return sshKey;
  }

  public void setSshKey(String sshKey) {
    this.sshKey = sshKey;
  }

  public String getInstanceType() {
    return instanceType;
  }

  public void setInstanceType(String instanceType) {
    this.instanceType = instanceType;
  }

  public String getUserData() {
    return userData;
  }

  public void setUserData(String userData) {
    this.userData = userData;
  }

  public String getNetwork() {
    return network;
  }

  public void setNetwork(String network) {
    this.network = network;
  }

  public String getSubnet() {
    return subnet;
  }

  public void setSubnet(String subnet) {
    this.subnet = subnet;
  }

  public List<String> getSecurityGroups() {
    return securityGroups;
  }

  public void setSecurityGroups(List<String> securityGroups) {
    this.securityGroups = securityGroups;
  }

  public Integer getInstanceBaseCount() {
    return instanceBaseCount;
  }

  public void setInstanceBaseCount(Integer instanceBaseCount) {
    this.instanceBaseCount = instanceBaseCount;
  }

  public Integer getInstanceMaxCount() {
    return instanceMaxCount;
  }

  public void setInstanceMaxCount(Integer instanceMaxCount) {
    this.instanceMaxCount = instanceMaxCount;
  }

  public List<AutoScalingPolicy> getScalingPolicies() {
    return scalingPolicies;
  }

  public void setScalingPolicies(List<AutoScalingPolicy> scalingPolicies) {
    this.scalingPolicies = scalingPolicies;
  }

  public List<Tag> getTags() {
    return tags;
  }

  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  public Integer getQuietPeriodSeconds() {
    return quietPeriodSeconds;
  }

  public String getUsername() {
    return username;
  }

  public String getAuthorizationRole() {
    return authorizationRole;
  }
}
