package com.nitorcreations.willow.metrics;

import static com.nitorcreations.willow.metrics.MetricUtils.median;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Named;

import com.nitorcreations.willow.messages.JmxMessage;
import com.nitorcreations.willow.messages.metrics.MetricConfig;


@Named("/heap")
public class HeapMemoryMetric extends FullMessageMultiseriesMetric<JmxMessage, Long, Long> {
  private static final String categoryPrefix = "category_jmx_";

  @Override
  protected void addValue(Map<String, SeriesData<Long, Long>> values, List<JmxMessage> preceeding, long stepTime, int stepLen, MetricConfig conf) {
    HashMap<String, List<Long>> data = new HashMap<>();
    for (JmxMessage next : preceeding) {
      String childName = "";
      for (String nextTag : next.tags) {
        if (nextTag.startsWith(categoryPrefix)) {
          childName = nextTag.substring(categoryPrefix.length());
          break;
        }
      }
      List<Long> nextData = data.get(childName);
      if (nextData == null) {
        nextData = new ArrayList<Long>();
        data.put(childName, nextData);
      }
      nextData.add(next.getHeapMemory());
    }
    for (Entry<String, List<Long>> next : data.entrySet()) {
      SeriesData<Long, Long> result = values.get(next.getKey());
      if (result == null) {
        result = new SeriesData<>();
        result.key = next.getKey();
        values.put(next.getKey(), result);
      }
      Point<Long, Long> nextPoint = new Point<>();
      nextPoint.x = stepTime;
      nextPoint.y = median(next.getValue());
      result.values.add(nextPoint);
    }
  }
}
