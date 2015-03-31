package com.nitorcreations.willow.deployer;

import java.util.Properties;

public interface StatisticsSender extends Runnable {
  public void setProperties(Properties properties);
}
