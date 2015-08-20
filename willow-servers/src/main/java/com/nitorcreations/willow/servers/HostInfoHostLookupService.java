package com.nitorcreations.willow.servers;

import java.util.Collection;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.nitorcreations.willow.messages.HostInfoMessage;
import com.nitorcreations.willow.messages.metrics.MetricConfig;
import com.nitorcreations.willow.metrics.HostInfoMetric;

public class HostInfoHostLookupService implements HostLookupService {

  private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

  public static final String PROPERTY_KEY_IP_ADDRESS_TYPE = "willow.hostLookupService.ipAddressType";

  @Inject
  HostInfoMetric hostInfoMetric;

  @Override
  public String getAdminUserFor(String tagHost) {
    logger.info(String.format("Resolving username for %s", tagHost));
    Collection<HostInfoMessage> hostInfoMessages = getHostInfoMessages(tagHost);
    if (hostInfoMessages != null && hostInfoMessages.size() > 0) {
      HostInfoMessage msg = hostInfoMessages.iterator().next();
      logger.info(String.format("Resolved %s username to %s", tagHost, msg.username));
      return msg.username;
    }
    logger.info("Could not resolve username using HostInfo metric, returning system username");
    return System.getProperty("user.name");
  }

  @Override
  public String getResolvableHostname(String tagHost) {
    logger.info(String.format("Resolving hostname for %s", tagHost));
    Collection<HostInfoMessage> hostInfoMessages = getHostInfoMessages(tagHost);
    for (HostInfoMessage msg : hostInfoMessages) {
      String hostname = getConfiguredHostname(msg);
      if (hostname != null) {
        logger.info(String.format("Resolved %s to %s", tagHost, hostname));
        return hostname;
      }
    }
    logger.info("Could not resolve hostname using HostInfo metric, returning host tag as is.");
    return tagHost;
  }

  private Collection<HostInfoMessage> getHostInfoMessages(String tagHost) {
    MetricConfig metricConfig = new MetricConfig();
    metricConfig.setTags("host_" + tagHost);
    long now = System.currentTimeMillis();
    metricConfig.setStart(now - 90000);
    metricConfig.setStop(now);
    return hostInfoMetric.calculateMetric(metricConfig);
  }

  private String getConfiguredHostname(HostInfoMessage him) {
    if ("PUBLIC".equalsIgnoreCase(System.getProperty(PROPERTY_KEY_IP_ADDRESS_TYPE, "PRIVATE"))) {
      return him.publicHostname;
    } else {
      return him.privateHostname;
    }
  }

  @Override
  public int getSshPort(String host) {
    return 22;
  }
}
