package com.nitorcreations.willow.metrics;

import static com.nitorcreations.willow.metrics.MetricUtils.median;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Named;

import com.nitorcreations.willow.messages.CPU;

@Named("/cpu")
public class CpuBusyMetric extends FullMessageSimpleMetric<CPU, Double> {
  Map<String, CPU> prevValues = new HashMap<>();

  @Override
  protected String getGroupBy(CPU message) {
    return "" + message.getFirstTagWithPrefix("host_");
  }
  @Override
  protected List<Double> filterGroupedData(HashMap<String, List<CPU>> groupedData) {
    Map<String, Double> hostBusy = new HashMap<>();
    for (Entry<String, List<CPU>> nextEntry : groupedData.entrySet()) {
      List<CPU> hostPrev = nextEntry.getValue();
      CPU last = hostPrev.get(hostPrev.size() - 1);
      long idleEnd = last.getIdle();
      long totalEnd = last.getTotal();
      long idleStart = hostPrev.get(0).getIdle();
      long totalStart = hostPrev.get(0).getTotal();
      if (hostPrev.size() == 1) {
        CPU prev = prevValues.get(nextEntry.getKey());
        if (prev != null) {
          idleStart = prev.getIdle();
          totalStart = prev.getTotal();
        }
      }
      prevValues.put(nextEntry.getKey(), last);
      long totalDiff = totalEnd - totalStart;
      if (totalDiff == 0) {
        hostBusy.put(nextEntry.getKey(), 0D);
      } else {
        long idleDiff = idleEnd - idleStart;
        Double nextBusy = (100 * (totalDiff - idleDiff)) / (double) totalDiff;
        hostBusy.put(nextEntry.getKey(), nextBusy);
      }
    }
    return new ArrayList<>(hostBusy.values());
  }
  @Override
  protected Double calculateValue(List<Double> values, long stepTime, long stepLen) {
    return median(values);
  }
  @Override
  protected Double fillMissingValue() {
    return 0D;
  }
}
