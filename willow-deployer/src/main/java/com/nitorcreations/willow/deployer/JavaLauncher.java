package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.*;

import java.io.File;
import java.util.Properties;

public class JavaLauncher extends AbstractLauncher {
	protected String mainClass="";
	protected File launchJar=null;
	protected String classPath="";


	@Override
	public void run() {
		launchArgs.add("-Daccesslog.websocket=" + statUri.toString());
		if (!mainClass.isEmpty() && !classPath.isEmpty()) {
			launchArgs.add("-cp");
			launchArgs.add(classPath);
			launchArgs.add(mainClass);
		} else {
			launchArgs.add("-jar");
			launchArgs.add(launchJar.getAbsolutePath());
		}
		addLauncherArgs(launchProperties, keyPrefix + PROPERTY_KEY_PREFIX_ARGS);
		super.run();
	}

	@Override
	public void setProperties(Properties properties, String keyPrefix) {
		super.setProperties(properties, keyPrefix);
		mainClass = properties.getProperty(keyPrefix + PROPERTY_KEY_MAIN_CLASS, "");
		classPath = properties.getProperty(keyPrefix + PROPERTY_KEY_CLASSPATH, "");
		String jarPath = properties.getProperty(keyPrefix + PROPERTY_KEY_JAR);
		if (jarPath != null && !jarPath.isEmpty()) {
			launchJar = new File(jarPath);
		}
		File javaBin = new File(new File(System.getProperty("java.home")), "bin");
		File java = null;
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			java = new File(javaBin, "java.exe");
		} else {
			java = new File(javaBin, "java");
		}

		launchArgs.add(java.getAbsolutePath());
		addLauncherArgs(properties, keyPrefix + PROPERTY_KEY_PREFIX_JAVA_ARGS);
	}
}
