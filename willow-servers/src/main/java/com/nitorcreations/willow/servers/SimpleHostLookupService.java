package com.nitorcreations.willow.servers;

public class SimpleHostLookupService implements HostLookupService {
  private String domainName;

  public SimpleHostLookupService() {
  }
  public SimpleHostLookupService(String domainName) {
    while (domainName.startsWith(".")) {
      domainName = domainName.substring(1);
    }
    this.domainName = domainName;
  }
  @Override
  public String getAdminUserFor(String tagHost) {
    return System.getProperty("user.name");
  }

  @Override
  public String getResolvableHostname(String tagHost) {
    if (domainName == null || domainName.isEmpty()) {
      return tagHost;
    } else {
      return tagHost + "." + domainName;
    }
  }

  public String getDomainName() {
    return domainName;
  }

  public void setDomainName(String domainName) {
    while (domainName.startsWith(".")) {
      domainName = domainName.substring(1);
    }
    this.domainName = domainName;
  }
  @Override
  public int getSshPort(String host) {
    return 22;
  }

}
