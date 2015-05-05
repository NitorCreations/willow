package com.nitorcreations.willow.servers;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.google.inject.AbstractModule;
import com.nitorcreations.willow.utils.HostUtil;

public class ElasticSearchModule extends AbstractModule {

  @Override
  protected void configure() {
    ImmutableSettings.Builder settingsBuilder = ImmutableSettings.settingsBuilder();
    String nodeName = System.getProperty("node.name", HostUtil.getHostName());
    if (nodeName == null || nodeName.isEmpty() || "localhost".equals(nodeName)) {
      nodeName = randomNodeId();
    }
    settingsBuilder.put("node.name", nodeName);
    settingsBuilder.put("path.data", System.getProperty("elasticsearch.path.data", "data/index"));
    String httpPort = System.getProperty("elasticsearch.http.port", null);
    if (httpPort != null) {
      settingsBuilder.put("http.enabled", true);
      settingsBuilder.put("http.port", Integer.parseInt(httpPort));
    }
    Settings settings = settingsBuilder.build();
    Node node = NodeBuilder.nodeBuilder().settings(settings).clusterName(System.getProperty("elasticsearch.cluster.name", "willowmetrics")).data(true).local(true).node();
    bind(Node.class).toInstance(node);
  }
  public String randomNodeId() {
    return new BigInteger(130, new SecureRandom()).toString(32);
  }
}
