package com.nitorcreations.willow.metrics;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.google.gson.Gson;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.nitorcreations.willow.utils.HostUtil;

@Singleton
public class MetricsServlet extends HttpServlet {
  private static final long serialVersionUID = -6704365246281136504L;
  private static Node node;
  ServletConfig config;
  private SecureRandom random = new SecureRandom();
  private Settings settings;

  @Inject
  protected Injector injector;
  private final Map<String, Metric> metrics;
  
  @Inject
  public MetricsServlet(Map<String, Metric> metrics) {
    this.metrics = new HashMap<>(metrics);
  }
  
  @Override
  public void init(ServletConfig config) throws ServletException {
    this.config = config;
    setupElasticSearch(config.getServletContext());
  }

  @Override
  public ServletConfig getServletConfig() {
    return config;
  }

  @Override
  public void service(ServletRequest req, ServletResponse res) throws IOException {
    if (!((HttpServletRequest) req).getMethod().equals("GET")) {
      ((HttpServletResponse) res).sendError(405, "Only GET allowed");
      return;
    }
    String metricKey = ((HttpServletRequest) req).getPathInfo();
    Metric metric = metrics.get(metricKey);
    if (metric == null) {
      ((HttpServletResponse) res).sendError(404, "Metric with key " + metricKey + " not found");
      return;
    }
    try {
      metric = metric.getClass().newInstance();
      Object data = metric.calculateMetric(getClient(), (HttpServletRequest) req);
      res.setContentType("application/json");
      Gson out = new Gson();
      res.getOutputStream().write(out.toJson(data).getBytes());
    } catch (InstantiationException | IllegalAccessException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String getServletInfo() {
    return "Cluster metrics";
  }

  @Override
  public void destroy() {
    node.stop();
  }

  public static Client getClient() {
    return node.client();
  }

  private void setupElasticSearch(ServletContext context) {
    ImmutableSettings.Builder settingsBuilder = ImmutableSettings.settingsBuilder();
    String nodeName = getInitParameter(context, "node.name", HostUtil.getHostName());
    if (nodeName == null || nodeName.isEmpty() || "localhost".equals(nodeName)) {
      nodeName = randomNodeId();
    }
    settingsBuilder.put("node.name", nodeName);
    settingsBuilder.put("path.data", getInitParameter(context, "path.data", "data/index"));
    String httpPort = getInitParameter(context, "http.port", null);
    if (httpPort != null) {
      settingsBuilder.put("http.enabled", true);
      settingsBuilder.put("http.port", Integer.parseInt(httpPort));
    }
    this.settings = settingsBuilder.build();
    node = NodeBuilder.nodeBuilder().settings(settings).clusterName(getInitParameter(context, "cluster.name", "metrics")).data(true).local(true).node();
  }

  public String getInitParameter(ServletContext context, String name, String defaultVal) {
    String ret = context.getInitParameter(name);
    if (ret == null || ret.isEmpty())
      return defaultVal;
    return ret;
  }
  public String randomNodeId() {
    return new BigInteger(130, random).toString(32);
  }
}
