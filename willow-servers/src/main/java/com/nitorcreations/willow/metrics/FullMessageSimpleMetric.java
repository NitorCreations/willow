package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.nitorcreations.willow.messages.AbstractMessage;

public abstract class FullMessageSimpleMetric<T extends AbstractMessage> extends FullMessageMetric<T, Collection<TimePoint>> {

  @Override
  public Collection<TimePoint> processData(long start, long stop, int step, MetricConfig conf) {
    int len = (int) ((stop - start) / step) + 1;
    List<TimePoint> ret = new ArrayList<TimePoint>();
    if (rawData.isEmpty())
      return ret;
    List<Long> retTimes = new ArrayList<Long>();
    long curr = start;
    for (int i = 0; i < len; i++) {
      retTimes.add(Long.valueOf(curr));
      curr += step;
    }
    for (Long nextTime : retTimes) {
      long afterNextTime = nextTime + 1;
      Collection<T> preceeding = rawData.headMap(afterNextTime).values();
      rawData = rawData.tailMap(afterNextTime);
      List<T> tmplist = new ArrayList<T>(preceeding);
      if (tmplist.isEmpty()) {
        ret.add(new TimePoint(nextTime.longValue(), fillMissingValue()));
        continue;
      }
      ret.add(new TimePoint(nextTime.longValue(), estimateValue(tmplist, nextTime, step)));
    }
    return ret;
  }
  protected Number fillMissingValue() {
    return 0D;
  }
  protected abstract Number estimateValue(List<T> preceeding, long stepTime, long stepLen);
}
