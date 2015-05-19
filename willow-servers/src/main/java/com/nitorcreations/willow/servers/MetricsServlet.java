package com.nitorcreations.willow.servers;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nitorcreations.willow.metrics.Metric;
import com.nitorcreations.willow.metrics.MetricConfig;

@Singleton
public class MetricsServlet extends HttpServlet {
  private static final long serialVersionUID = -6704365246281136504L;
  @Inject
  private transient Node node;
  @Inject
  private transient Map<String, Metric> metrics;
  private ServletConfig config;

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
    MetricConfig conf = new MetricConfig(req);
    Metric metric = metrics.get(conf.metricKey);
    if (metric == null) {
      ((HttpServletResponse) res).sendError(404, "Metric with key " + conf.metricKey + " not found");
      return;
    }
    try (Client client = node.client()){
      metric = metric.getClass().newInstance();
      Object data = metric.calculateMetric(client, conf);
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
