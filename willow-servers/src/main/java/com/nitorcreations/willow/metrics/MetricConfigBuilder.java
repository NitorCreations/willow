package com.nitorcreations.willow.metrics;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import com.nitorcreations.willow.messages.metrics.MetricConfig;

@Named
public class MetricConfigBuilder {

  public MetricConfig fromRequest(HttpServletRequest req) {
    MetricConfig config = new MetricConfig();
    long now = System.currentTimeMillis();
    config.setMetricKey(req.getPathInfo());
    config.setStart(getLongParameter(req, "start", now - 30000));
    config.setStop(getLongParameter(req, "stop", now));
    config.setStep((int)getLongParameter(req, "step", 0));
    config.setMinSteps((int)getLongParameter(req, "minsteps", 1));
    config.setTypes(getListParameter(req, "type"));
    config.setLimits(getListParameter(req, "limits"));
    config.setTags(getListParameter(req, "tag"));
    return config;
  }

  private long getLongParameter(HttpServletRequest req, String name, long def) {
    String attr = req.getParameter(name);
    if (attr == null) {
      return def;
    }
    try {
      return Long.parseLong(attr);
    } catch (NumberFormatException e) {
      return def;
    }
  }

  private String[] getListParameter(HttpServletRequest req, String name) {
    String[] ret = req.getParameterValues(name);
    if (ret == null) {
      return new String[0];
    }
    return ret;
  }
}
