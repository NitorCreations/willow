package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.nitorcreations.willow.messages.AbstractMessage;
import com.nitorcreations.willow.messages.metrics.MetricConfig;
import com.nitorcreations.willow.messages.metrics.TimePoint;

public abstract class FullMessageSimpleMetric<T extends AbstractMessage, Y extends Comparable> extends FullMessageMetric<T, Collection<TimePoint<Y>>> {

  @Override
  public Collection<TimePoint<Y>> processData(long start, long stop, int step, MetricConfig conf) {
    List<TimePoint<Y>> ret = new ArrayList<TimePoint<Y>>();
    if (rawData.isEmpty())
      return ret;
    List<Long> retTimes = new LinkedList<Long>();
    long curr = stop;
    while (curr > start) {
      retTimes.add(0, Long.valueOf(curr));
      curr -= step;
    }
    rawData = rawData.tailMap(curr - step + 1); 
    for (Long nextTime : retTimes) {
      long afterNextTime = nextTime + 1;
      Collection<T> preceeding = rawData.headMap(afterNextTime).values();
      rawData = rawData.tailMap(afterNextTime);
      List<T> tmplist = new ArrayList<T>(preceeding);
      if (tmplist.isEmpty()) {
        ret.add(new TimePoint<Y>(nextTime.longValue(), fillMissingValue()));
        continue;
      }
      ret.add(new TimePoint<Y>(nextTime.longValue(), estimateValue(tmplist, nextTime, step)));
    }
    return ret;
  }
  protected abstract Y fillMissingValue();

  protected Y estimateValue(List<T> preceeding, long stepTime, long stepLen) {
    return calculateValue(filterGroupedData(groupData(preceeding)), stepTime, stepLen);
  }
  protected HashMap<String, List<T>> groupData(List<T> preceeding) {
    HashMap<String, List<T>> groupedData = new HashMap<>();
    for (T next : preceeding) {
      String host = "" + getGroupBy(next);
      List<T> groupMessages = groupedData.get(host);
      if (groupMessages == null) {
        groupMessages = new ArrayList<>();
        groupedData.put(host, groupMessages);
      }
      groupMessages.add(next);
    }
    return groupedData;
  }

  protected abstract String getGroupBy(T message);
  protected abstract List<Y> filterGroupedData(HashMap<String, List<T>> groupedData);
  protected abstract Y calculateValue(List<Y> values, long stepTime, long stepLen);
}
