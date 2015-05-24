package com.nitorcreations.willow.servers;

import java.util.Collection;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.elasticsearch.node.Node;

import com.nitorcreations.willow.messages.HostInfoMessage;
import com.nitorcreations.willow.messages.metrics.MetricConfig;
import com.nitorcreations.willow.metrics.HostInfoMetric;

public class HostInfoHostLookupService implements HostLookupService {

  private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

  @Inject
  Node node;
  @Inject
  HostInfoMetric hostInfoMetric;

  @Override
  public String getAdminUserFor(String tagHost) {
    return System.getProperty("user.name");
  }

  @Override
  public String getResolvableHostname(String tagHost) {
    MetricConfig metricConfig = new MetricConfig();
    metricConfig.setTags("host_" + tagHost);
    Collection<HostInfoMessage> hostInfoMessages = hostInfoMetric.calculateMetric(node.client(), metricConfig);
    for (HostInfoMessage msg : hostInfoMessages) {
      if (msg.getInstance().equals(tagHost)) {
        logger.info(String.format("Resolving %s to %s", tagHost, msg.publicHostname));
        return msg.privateHostname; //TODO return public or private based on configuration
      }
    }
    return tagHost;
  }

  @Override
  public int getSshPort(String host) {
    return 22;
  }
}
