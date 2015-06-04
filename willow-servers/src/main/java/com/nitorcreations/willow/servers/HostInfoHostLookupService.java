package com.nitorcreations.willow.servers;

import com.nitorcreations.willow.messages.HostInfoMessage;
import com.nitorcreations.willow.messages.metrics.MetricConfig;
import com.nitorcreations.willow.metrics.HostInfoMetric;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

public class HostInfoHostLookupService implements HostLookupService {

  private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

  @Inject
  HostInfoMetric hostInfoMetric;

  @Override
  public String getAdminUserFor(String tagHost) {
    logger.info(String.format("Resolving username for %s", tagHost));
    Collection<HostInfoMessage> hostInfoMessages = getHostInfoMessages(tagHost);
    for (HostInfoMessage msg : hostInfoMessages) {
      if (msg.getInstance().equals(tagHost)) {
        logger.info(String.format("Resolved %s username to %s", tagHost, msg.username));
        return msg.username;
      }
    }
    logger.info("Could not resolve username using HostInfo metric, returning system username");
    return System.getProperty("user.name");
  }

  @Override
  public String getResolvableHostname(String tagHost) {
    logger.info(String.format("Resolving hostname for %s", tagHost));
    Collection<HostInfoMessage> hostInfoMessages = getHostInfoMessages(tagHost);
    for (HostInfoMessage msg : hostInfoMessages) {
      if (msg.getInstance().equals(tagHost)) {
        logger.info(String.format("Resolved %s to %s", tagHost, msg.privateHostname));
        return msg.privateHostname; //TODO return public or private based on configuration
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

  @Override
  public int getSshPort(String host) {
    return 22;
  }
}
