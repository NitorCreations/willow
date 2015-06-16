package com.nitorcreations.willow.metrics;

import static com.nitorcreations.willow.metrics.MetricUtils.sumLong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Named;

import com.nitorcreations.willow.messages.DiskIO;

@Named("/diskio")
public class DiskIOMetric extends FullMessageSimpleMetric<DiskIO, Long> {
  Map<String, DiskIO> prevValues = new HashMap<>();

  @Override
  protected Long fillMissingValue() {
    return 0L;
  }

  @Override
  protected String getGroupBy(DiskIO message) {
    return message.name;
  }

  @Override
  protected List<Long> filterGroupedData(HashMap<String, List<DiskIO>> groupedData) {
    Map<String, Long> deviceData = new HashMap<>();
    for (Entry<String, List<DiskIO>> nextEntry : groupedData.entrySet()) {
      List<DiskIO> hostPrev = nextEntry.getValue();
      DiskIO last = hostPrev.get(hostPrev.size() - 1);
      long readEnd = last.getReadBytes();
      long writtenEnd = last.getWriteBytes();
      long readStart = hostPrev.get(0).getReadBytes();
      long writeStart = hostPrev.get(0).getWriteBytes();
      if (hostPrev.size() == 1) {
        DiskIO prev = prevValues.get(nextEntry.getKey());
        if (prev != null) {
          readStart = prev.getReadBytes();
          writeStart = prev.getWriteBytes();
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

  @Override
  protected Long calculateValue(List<Long> values, long stepTime, long stepLen) {
    return (1000 * sumLong(values)) / stepLen;
  }
}
