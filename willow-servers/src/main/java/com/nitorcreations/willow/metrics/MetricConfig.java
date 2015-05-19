package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

public class MetricConfig {
  public static final int MIN_STEPLEN = 1000;
  public static final long MAX_TIMEPERIOD = TimeUnit.DAYS.toMillis(30);
  public String metricKey = "";
  private long start;
  private long stop;
  private int step;
  private int minSteps;
  private final List<String> types = new ArrayList<>();
  private final List<String> limits = new ArrayList<>();;
  private final List<String> tags = new ArrayList<>();;

  public String getMetricKey() {
    return metricKey;
  }
  public void setMetricKey(String metricKey) {
    this.metricKey = metricKey;
  }
  public long getStart() {
	long ret;
    if (stop - (minSteps * step) < start) {
      ret = stop - (minSteps * step);
    } else {
      ret = start;
    }
    if ((stop - ret) > MAX_TIMEPERIOD) {
    	return stop - MAX_TIMEPERIOD;
    } else {
    	return ret;
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
    return Math.max(step, MIN_STEPLEN);
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
    return types.toArray(new String[types.size()]);
  }
  public void setTypes(String... types) {
    this.types.clear();
    addTypes(types);
  }
  public void addTypes(String... types) {
    this.types.addAll(Arrays.asList(types));
  }
  public String[] getLimits() {
    return limits.toArray(new String[limits.size()]);
  }
  public void setLimits(String... limits) {
    this.limits.clear();
    addLimits(limits);
  }
  public void addLimits(String... limits) {
    this.limits.addAll(Arrays.asList(limits));
  }
  public String[] getTags() {
    return tags.toArray(new String[tags.size()]);
  }
  public void setTags(String... tags) {
    this.tags.clear();
    addTags(tags);
  }
  public void addTags(String... tags) {
    this.tags.addAll(Arrays.asList(tags));
  }
  public MetricConfig() {
    long now = System.currentTimeMillis();
    start = now - 30000;
    stop = now;
    step = 0;
    minSteps = 1;
  }
  public MetricConfig(HttpServletRequest req) {
    long now = System.currentTimeMillis();
    metricKey = req.getPathInfo();
    start = getLongParameter(req, "start", now - 30000);
    stop = getLongParameter(req, "stop", now);
    step = (int)getLongParameter(req, "step", 0);
    minSteps = (int)getLongParameter(req, "minsteps", 1);
    setTypes(getListParameter(req, "type"));
    setLimits(getListParameter(req, "limits"));
    setTags(getListParameter(req, "tag"));
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
    if (types.size() == 0 || type == null) return false;
    return types.contains(type);
  }
}
