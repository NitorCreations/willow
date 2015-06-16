package com.nitorcreations.willow.servers;

public interface HostLookupService {
  String getAdminUserFor(String tagHost);
  String getResolvableHostname(String tagHost);
  int getSshPort(String host);
}
