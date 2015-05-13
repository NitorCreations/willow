package com.nitorcreations.willow.servers;

public interface HostLookupService {
	public String getAdminUserFor(String tagHost);
	public String getResolvableHostname(String tagHost);
	public int getSshPort(String host);
}
