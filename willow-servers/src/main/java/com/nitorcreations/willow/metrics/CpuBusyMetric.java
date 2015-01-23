package com.nitorcreations.willow.metrics;

import java.util.List;

public class CpuBusyMetric extends SimpleMetric<CpuData, Long> {
  CpuData prevValues;
  double prevRes = 0D;

  @Override
  protected CpuData getValue(List<Long> results) {
    CpuData ret = new CpuData(((Number) results.get(0)).longValue(), ((Number) results.get(1)).longValue());
    if (prevValues == null)
      prevValues = ret;
    return ret;
  }

  @Override
  public String getType() {
    return "cpu";
  }

  @Override
  public String[] requiresFields() {
    return new String[] { "idle", "total" };
  }

  @Override
  protected Double estimateValue(List<CpuData> preceeding, long stepTime, long stepLen) {
    CpuData last = preceeding.get(preceeding.size() - 1);
    long idleEnd = last.idle;
    long totalEnd = last.total;
    long idleStart = preceeding.get(0).idle;
    long totalStart = preceeding.get(0).total;
    if (preceeding.size() == 1) {
      idleStart = prevValues.idle;
      totalStart = prevValues.total;
    }
    prevValues = last;
    long totalDiff = totalEnd - totalStart;
    if (totalDiff == 0)
      return fillMissingValue();
    long idleDiff = idleEnd - idleStart;
    return prevRes = (100 * (totalDiff - idleDiff)) / (double) totalDiff;
  }

  @Override
  protected Double fillMissingValue() {
    return 0D;
  }
}
