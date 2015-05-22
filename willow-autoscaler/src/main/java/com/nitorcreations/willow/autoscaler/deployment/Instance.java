package com.nitorcreations.willow.autoscaler.deployment;

import com.nitorcreations.willow.autoscaler.config.Tag;

import java.util.List;

public class Instance {

  private final String instanceId;
  private String publicIp;
  private String publicHostname;
  private String privateIp;
  private String privateHostname;
  private String instanceType;
  private List<Tag> tags;

  public Instance (String instanceId) {
    this.instanceId = instanceId;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getPublicIp() {
    return publicIp;
  }

  public Instance setPublicIp(String publicIp) {
    this.publicIp = publicIp;
    return this;
  }

  public String getPublicHostname() {
    return publicHostname;
  }

  public Instance setPublicHostname(String publicHostname) {
    this.publicHostname = publicHostname;
    return this;
  }

  public String getPrivateIp() {
    return privateIp;
  }

  public Instance setPrivateIp(String privateIp) {
    this.privateIp = privateIp;
    return this;
  }

  public String getPrivateHostname() {
    return privateHostname;
  }

  public Instance setPrivateHostname(String privateHostname) {
    this.privateHostname = privateHostname;
    return this;
  }

  public String getInstanceType() {
    return instanceType;
  }

  public Instance setInstanceType(String instanceType) {
    this.instanceType = instanceType;
    return this;
  }

  public List<Tag> getTags() {
    return tags;
  }

  public Instance setTags(List<Tag> tags) {
    this.tags = tags;
    return this;
  }
}
