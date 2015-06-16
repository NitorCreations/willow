package com.nitorcreations.willow.metrics;

import static com.nitorcreations.willow.metrics.MetricUtils.median;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Named;

import com.nitorcreations.willow.messages.ProcessCPU;
import com.nitorcreations.willow.messages.metrics.MetricConfig;

@Named("/childcpu")
public class ChildCpuMetric extends FullMessageMultiseriesMetric<ProcessCPU, Long, Double> {
  private static final String categoryPrefix = "category_processcpu_";
  @Override
  protected void addValue(Map<String, SeriesData<Long, Double>> values, List<ProcessCPU> preceeding, long stepTime, int stepLen, MetricConfig conf) {
    HashMap<String, List<Double>> data = new HashMap<>();
    for (ProcessCPU next : preceeding) {
      String childName = next.getFirstTagWithPrefix(categoryPrefix);
      if (childName != null) {
        childName = childName.substring(categoryPrefix.length());
      }
      List<Double> nextData = data.get(childName);
      if (nextData == null) {
        nextData = new ArrayList<Double>();
        data.put(childName, nextData);
      }
      nextData.add(next.getPercent());
    }
    for (Entry<String, List<Double>> next : data.entrySet()) {
      SeriesData<Long, Double> result = values.get(next.getKey());
      if (result == null) {
        result = new SeriesData<>();
        result.key = next.getKey();
        values.put(next.getKey(), result);
      }
      Point<Long, Double> nextPoint = new Point<>();
      nextPoint.x = stepTime;
      nextPoint.y = median(next.getValue());
      result.values.add(nextPoint);
    }
  }
}
