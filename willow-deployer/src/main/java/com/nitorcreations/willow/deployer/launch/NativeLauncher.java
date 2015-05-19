package com.nitorcreations.willow.deployer.launch;

import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_ARGS;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_BINARY;

import javax.inject.Named;

import com.nitorcreations.willow.utils.MergeableProperties;

@Named
public class NativeLauncher extends AbstractLauncher {
  @Override
  public void setProperties(MergeableProperties properties, LaunchCallback callback) {
    super.setProperties(properties, callback);
    launchArgs.add(properties.getProperty(PROPERTY_KEY_SUFFIX_BINARY));
    addLauncherArgs(properties, PROPERTY_KEY_SUFFIX_ARGS);
  }
}
