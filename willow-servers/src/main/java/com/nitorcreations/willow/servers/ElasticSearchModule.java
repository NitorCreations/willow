package com.nitorcreations.willow.servers;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.google.inject.AbstractModule;
import com.nitorcreations.willow.utils.HostUtil;

public class ElasticSearchModule extends AbstractModule {
  private final Node node;
  private final Client client;
  public ElasticSearchModule() {
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
    String clusterName = System.getProperty("elasticsearch.cluster.name");
    NodeBuilder builder = NodeBuilder.nodeBuilder().settings(settings);
    if (clusterName == null) {
      builder.clusterName("willowmetrics").data(true).local(true);
    } else {
      builder.clusterName(clusterName).data(false).client(true).local(false);
    }
    node = builder.node();
    client = node.client();
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        client.close();
        node.close();
      }
    });
  }
  @Override
  protected void configure() {
    bind(Client.class).toInstance(client);
  }
  public String randomNodeId() {
    return new BigInteger(130, new SecureRandom()).toString(32);
  }
  
}
