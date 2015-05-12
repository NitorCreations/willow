package com.nitorcreations.willow.metrics;

import javax.servlet.http.HttpServletRequest;

public class MetricConfig {
  public String metricKey;
  private long start;
  private long stop;
  private int step;
  private int minSteps;
  private String[] types;
  private String[] limits;
  private String[] tags;

  public String getMetricKey() {
    return metricKey;
  }
  public void setMetricKey(String metricKey) {
    this.metricKey = metricKey;
  }
  public long getStart() {
    if (stop - (minSteps * step) < start) {
      return stop - (minSteps * step);
    } else {
      return start;
    }
  }
  public void setStart(long start) {
    this.start = start;
  }
  public long getStop() {
    return stop;
  }
  public void setStop(long stop) {
    this.stop = stop;
  }
  public int getStep() {
    return step;
  }
  public void setStep(int step) {
    this.step = step;
  }
  public int getMinSteps() {
    return minSteps;
  }
  public void setMinSteps(int minSteps) {
    this.minSteps = minSteps;
  }
  public String[] getTypes() {
    return types;
  }
  public void setTypes(String[] types) {
    if (types == null) {
      this.types = new String[0];
    } else {
      this.types = types;
    }
  }
  public String[] getLimits() {
    return limits;
  }
  public void setLimits(String[] limits) {
    if (limits == null) {
      this.limits = new String[0];
    } else {
      this.limits = limits;
    }
  }
  public String[] getTags() {
    return tags;
  }
  public void setTags(String[] tags) {
    if (tags == null) {
      this.tags = new String[0];
    } else {
      this.tags = tags;
    }
  }
  public MetricConfig(HttpServletRequest req) {
    long now = System.currentTimeMillis();
    metricKey = req.getPathInfo();
    start = getLongParameter(req, "start", now - 30000);
    stop = getLongParameter(req, "stop", now);
    step = (int)getLongParameter(req, "step", 0);
    minSteps = (int)getLongParameter(req, "minsteps", 1);
    types = getListParameter(req, "type");
    limits = getListParameter(req, "limits");
    tags = getListParameter(req, "tag");
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
  protected String[] getListParameter(HttpServletRequest req, String name) {
    String[] ret = req.getParameterValues(name);
    if (ret == null) return new String[0];
    return ret;
  }
  public boolean hasType(String type) {
    if (types == null || types.length == 0 || type == null) return false;
    for (String myType : types) {
      if (type.equals(myType)) return true;
    }
    return false;
  }
}
