package com.nitorcreations.willow.servers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.node.Node;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Injector;
import com.nitorcreations.willow.metrics.Metric;

@Singleton
public class MetricsServlet extends HttpServlet {
  private static final long serialVersionUID = -6704365246281136504L;
  @Inject
  private Node node;
  ServletConfig config;

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
  }

  @Override
  public ServletConfig getServletConfig() {
    return config;
  }

  @Override
  public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
    if (!req.getMethod().equals("GET")) {
      res.sendError(405, "Only GET allowed");
      return;
    }
    String metricKey = req.getPathInfo();
    Metric metric = metrics.get(metricKey);
    if (metric == null) {
      ((HttpServletResponse) res).sendError(404, "Metric with key " + metricKey + " not found");
      return;
    }
    try {
      metric = metric.getClass().newInstance();
      Object data = metric.calculateMetric(node.client(), req);
      res.setContentType("application/json");
      Gson out;
      if ("true".equals(req.getAttribute("pretty"))) {
        out = new GsonBuilder().setPrettyPrinting().create();
      } else {
        out = new Gson();
      }
      out.toJson(data, res.getWriter());
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
}
