package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nitorcreations.willow.messages.AbstractMessage;
import com.nitorcreations.willow.messages.metrics.MetricConfig;

public abstract class FullMessageMultiseriesMetric<T extends AbstractMessage, X extends Comparable<X>, Y extends Comparable<Y>> extends FullMessageMetric<T, Collection<SeriesData<X, Y>>>{

  @Override
  public Collection<SeriesData<X, Y>> processData(long start, long stop, int step, MetricConfig conf) {
    int len = (int) ((stop - start) / step) + 1;
    Map<String, SeriesData<X, Y>> ret = new LinkedHashMap<String, SeriesData<X, Y>>();
    if (rawData.isEmpty())
      return ret.values();
    List<Long> retTimes = new ArrayList<Long>();
    long curr = start;
    for (int i = 0; i < len; i++) {
      retTimes.add(curr);
      curr += step;
    }
    for (Long nextTime : retTimes) {
      long afterNextTime = nextTime + 1;
      Collection<T> preceeding = rawData.headMap(afterNextTime).values();
      rawData = rawData.tailMap(afterNextTime);
      List<T> tmplist = new ArrayList<T>(preceeding);
      addValue(ret, tmplist, nextTime.longValue(), step, conf);
    }
    fillMissingValues(ret, retTimes, step);
    return ret.values();
  }

  protected abstract void addValue(Map<String, SeriesData<X, Y>> values, List<T> preeceding, long stepTime, int stepLen, MetricConfig conf);

  @SuppressWarnings("unchecked")
  protected void fillMissingValues(Map<String, SeriesData<X, Y>> ret, List<Long> retTimes, int stepLen) {
    for (SeriesData<X, Y> nextValues : ret.values()) {
      Map<X, Y> valueMap = nextValues.pointsAsMap();
      List<Long> addX = new ArrayList<>();
      for (Long nextStep : retTimes) {
        if (!valueMap.containsKey(nextStep)) {
          addX.add(nextStep);
        }
      }
      Object zero = 0;
      for (Long nextAdd : addX) {
        for (int i = 0; i < nextValues.values.size(); i++) {
          if (nextValues.values.get(i).x.compareTo((X) nextAdd) > 0) {
            Point<X, Y> toAdd = new Point<>();
            toAdd.x = (X) nextAdd;
            toAdd.y = (Y) zero;
            nextValues.values.add(i, toAdd);
            break;
          }
        }
      }
    }
  }
}
