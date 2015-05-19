package com.nitorcreations.willow.deployer.statistics;

import java.util.Properties;

public interface StatisticsSender extends Runnable {
  void setProperties(Properties properties);
  void stop();
}
