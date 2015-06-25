package com.nitorcreations.willow.deployer.statistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import com.nitorcreations.willow.deployer.Main;

public abstract class AbstractChildStatisticsSender extends AbstractStatisticsSender {
  @Inject
  protected Main main;
  protected List<String> configuredChildren = new ArrayList<String>();

  @Override
  public void setProperties(Properties properties) {
    String nextChild = properties.getProperty("children[0]");
    int i = 0;
    while (nextChild != null) {
      configuredChildren.add(nextChild);
      nextChild = properties.getProperty("children[" + ++i + "]");
    }
  }
  protected List<String> getChildren() {
    if (configuredChildren.isEmpty()) {
      return Arrays.asList(main.getChildNames());
    } else {
      return Collections.unmodifiableList(configuredChildren);
    }
  }

}
