package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_BINARY;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_ARGS;

import com.nitorcreations.willow.utils.MergeableProperties;

public class NativeLauncher extends AbstractLauncher implements LaunchMethod {
  @Override
  public void setProperties(MergeableProperties properties, String keyPrefix) {
    super.setProperties(properties, keyPrefix);
    launchArgs.add(properties.getProperty(keyPrefix + PROPERTY_KEY_BINARY));
    addLauncherArgs(properties, keyPrefix + PROPERTY_KEY_PREFIX_ARGS);
  }
}
