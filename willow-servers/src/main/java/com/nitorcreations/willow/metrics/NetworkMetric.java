package com.nitorcreations.willow.metrics;

import static com.nitorcreations.willow.metrics.MetricUtils.sumLong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Named;

import com.nitorcreations.willow.messages.NetInterface;

@SuppressWarnings("CPD-START")
@Named("/net")
public class NetworkMetric extends FullMessageSimpleMetric<NetInterface, Long> {
  Map<String, NetInterface> prevValues = new HashMap<>();

  @Override
  protected Long fillMissingValue() {
    return 0L;
  }

  @Override
  protected String getGroupBy(NetInterface message) {
    return message.getName();
  }
  @Override
  protected List<Long> filterGroupedData(HashMap<String, List<NetInterface>> groupedData) {
    Map<String, Long> deviceData = new HashMap<>();
    for (Entry<String, List<NetInterface>> nextEntry : groupedData.entrySet()) {
      List<NetInterface> hostPrev = nextEntry.getValue();
      NetInterface last = hostPrev.get(hostPrev.size() - 1);
      long readEnd = last.getRxBytes();
      long writtenEnd = last.getTxBytes();
      long readStart = hostPrev.get(0).getRxBytes();
      long writeStart = hostPrev.get(0).getTxBytes();
      if (hostPrev.size() == 1) {
        NetInterface prev = prevValues.get(nextEntry.getKey());
        if (prev != null) {
          readStart = prev.getRxBytes();
          writeStart = prev.getTxBytes();
        }
      }
      prevValues.put(nextEntry.getKey(), last);
      long readDiff = readEnd - readStart;
      long writtenDiff = writtenEnd - writeStart;
      long sum = readDiff + writtenDiff;
      if (sum < 0) {
        sum = 0;
      }
      deviceData.put(nextEntry.getKey(), sum);
    }
    return new ArrayList<>(deviceData.values());
  }
  @SuppressWarnings("CPD-END")
  @Override
  protected Long calculateValue(List<Long> values, long stepTime, long stepLen) {
    return (1000 * sumLong(values)) / stepLen;
  }
}
