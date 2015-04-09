package com.nitorcreations.willow.deployer.statistics;

import java.util.Properties;

public interface StatisticsSender extends Runnable {
  public void setProperties(Properties properties);
  public void stop();
}
