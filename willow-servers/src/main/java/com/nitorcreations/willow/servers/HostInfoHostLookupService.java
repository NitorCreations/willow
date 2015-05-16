package com.nitorcreations.willow.servers;

import com.nitorcreations.willow.messages.HostInfoMessage;
import com.nitorcreations.willow.metrics.HostInfoMetric;
import com.nitorcreations.willow.metrics.MetricConfig;
import org.elasticsearch.node.Node;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

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
      if (msg.instance.equals(tagHost)) {
        logger.info(String.format("Resolving %s to %s", tagHost, msg.privateHostname));
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
