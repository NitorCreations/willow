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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Injector;
import com.nitorcreations.willow.messages.metrics.MetricConfig;
import com.nitorcreations.willow.metrics.Metric;
import com.nitorcreations.willow.metrics.MetricConfigBuilder;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Singleton
@SuppressFBWarnings(value={"SE_TRANSIENT_FIELD_NOT_RESTORED"}, justification="metrics set by guice")
public class MetricsServlet extends HttpServlet {
  private static final long serialVersionUID = -6704365246281136504L;
  @Inject
  private transient Map<String, Metric> metrics;
  @Inject
  private transient MetricConfigBuilder metricConfigBuilder;
  @Inject
  private transient Injector injector;

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
    MetricConfig conf = metricConfigBuilder.fromRequest(req);
    Metric metric = metrics.get(conf.getMetricKey());
    if (metric == null) {
      res.sendError(404, "Metric with key " + conf.getMetricKey() + " not found");
      return;
    }
    try {
      metric = metric.getClass().newInstance();
      injector.injectMembers(metric);
      Object data = metric.calculateMetric(conf);
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
}
