package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Named;

import com.nitorcreations.willow.messages.CPU;

@Named("/cpu")
public class CpuBusyMetric extends FullMessageSimpleMetric<CPU> {
  Map<String, CPU> prevValues = new HashMap<>();


  @Override
  protected Number estimateValue(List<CPU> preceeding, long stepTime, long stepLen) {
    HashMap<String, List<CPU>> perHostCPUData = new HashMap<>();
    for (CPU next : preceeding) {
      String host = "" + next.getFirstTagWithPrefix("host_");
      List<CPU> hostCpu = perHostCPUData.get(host);
      if (hostCpu == null) {
        hostCpu = new ArrayList<>();
        perHostCPUData.put(host, hostCpu);
      }
      hostCpu.add(next);
    }
    Map<String, Double> hostBusy = new HashMap<>();
    for (Entry<String, List<CPU>> nextEntry : perHostCPUData.entrySet()) {
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
    return median(new ArrayList<>(hostBusy.values()));
  }
}
