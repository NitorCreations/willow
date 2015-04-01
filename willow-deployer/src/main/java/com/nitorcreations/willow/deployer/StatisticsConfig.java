package com.nitorcreations.willow.deployer;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class StatisticsConfig {
  private final long intervalProcs;
  private final long intervalCpus;
  private final long intervalProcCpus;
  private final long intervalMem;
  private final long intervalJmx;
  private final long intervalDisks;
  private final long intervalNet;
  private final long intervalDiskIO;
  private final long intervalNetStat;

  public StatisticsConfig() {
    this.intervalProcs = TimeUnit.SECONDS.toMillis(5);
    this.intervalCpus = TimeUnit.SECONDS.toMillis(5);
    this.intervalProcCpus = TimeUnit.SECONDS.toMillis(5);
    this.intervalMem = TimeUnit.SECONDS.toMillis(5);
    this.intervalJmx = TimeUnit.SECONDS.toMillis(5);
    this.intervalNet = TimeUnit.SECONDS.toMillis(5);
    this.intervalNetStat = TimeUnit.SECONDS.toMillis(5);
    this.intervalDisks = TimeUnit.MINUTES.toMillis(1);
    this.intervalDiskIO = TimeUnit.SECONDS.toMillis(5);
  }

  public StatisticsConfig(long intervalProcs, long intervalCpus, long intervalProcCpus, long intervalMem, long intervalJmx, long intervalDisks, long intervalNet, long intervalNetStat, long intervalDiskIO) {
    this.intervalProcs = intervalProcs;
    this.intervalCpus = intervalCpus;
    this.intervalProcCpus = intervalProcCpus;
    this.intervalMem = intervalMem;
    this.intervalJmx = intervalJmx;
    this.intervalDisks = intervalDisks;
    this.intervalNet = intervalNet;
    this.intervalNetStat = intervalNetStat;
    this.intervalDiskIO = intervalDiskIO;
  }

  public StatisticsConfig(Properties properties) {
    this.intervalProcs = Long.parseLong(properties.getProperty("intervalProcs", "5000"));
    this.intervalCpus = Long.parseLong(properties.getProperty("intervalCpus", "5000"));
    this.intervalProcCpus = Long.parseLong(properties.getProperty("intervalProcCpus", "5000"));
    this.intervalMem = Long.parseLong(properties.getProperty("intervalMem", "5000"));
    this.intervalJmx = Long.parseLong(properties.getProperty("intervalJmx", "5000"));
    this.intervalDisks = Long.parseLong(properties.getProperty("intervalDisks", "5000"));
    this.intervalNet = Long.parseLong(properties.getProperty("intervalNet", "5000"));
    this.intervalNetStat = Long.parseLong(properties.getProperty("intervalNetStat", "5000"));
    this.intervalDiskIO = Long.parseLong(properties.getProperty("intervalDiskIO", "5000"));
  }

  public long getIntervalProcs() {
    return intervalProcs;
  }

  public long getIntervalCpus() {
    return intervalCpus;
  }

  public long getIntervalProcCpus() {
    return intervalProcCpus;
  }

  public long getIntervalMem() {
    return intervalMem;
  }

  public long getIntervalJmx() {
    return intervalJmx;
  }

  public long getIntervalDisks() {
    return intervalDisks;
  }

  public long getIntervalNet() {
    return intervalNet;
  }

  public long shortest() {
    return min(intervalCpus, intervalDisks, intervalJmx, intervalMem, intervalProcCpus, intervalProcs, intervalNet, intervalNetStat, intervalDiskIO);
  }

  private static long min(long... vals) {
    long ret = Long.MAX_VALUE;
    for (long next : vals) {
      if (next < ret)
        ret = next;
    }
    return ret;
  }

  public long getIntervalDiskIO() {
    return intervalDiskIO;
  }

  public long getIntervalNetStat() {
    return intervalNetStat;
  }
}
