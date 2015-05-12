package com.nitorcreations.willow.metrics;

import javax.servlet.http.HttpServletRequest;

public class MetricConfig {
  public String metricKey;
  public long start;
  public long stop;
  public int step;
  public String[] types;
  public String[] limits;
  public String[] tags;

  public MetricConfig(HttpServletRequest req) {
    long now = System.currentTimeMillis();
    start = getLongParameter(req, "start", now - 30000);
    stop = getLongParameter(req, "stop", now);
    step = (int)getLongParameter(req, "step", 0);
    types = req.getParameterValues("type");
    limits = req.getParameterValues("limits");
    tags = req.getParameterValues("tag");
    metricKey = req.getPathInfo();
  }
  protected long getLongParameter(HttpServletRequest req, String name, long def) {
    String attr = req.getParameter(name);
    if (attr == null) return def;
    try {
      return Long.parseLong(attr);
    } catch (NumberFormatException e) {
      return def;
    }
  }
  public boolean hasType(String type) {
    if (types == null || types.length == 0 || type == null) return false;
    for (String myType : types) {
      if (type.equals(myType)) return true;
    }
    return false;
  }

}
