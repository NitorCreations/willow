package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.*;

import java.util.Properties;

public class NativeLauncher extends AbstractLauncher implements LaunchMethod {

	@Override
	public void setProperties(Properties properties, String keyPrefix) {
		super.setProperties(properties, keyPrefix);
		launchArgs.add(properties.getProperty(keyPrefix + PROPERTY_KEY_BINARY));
		addLauncherArgs(properties, keyPrefix + PROPERTY_KEY_PREFIX_ARGS);
	}


}
