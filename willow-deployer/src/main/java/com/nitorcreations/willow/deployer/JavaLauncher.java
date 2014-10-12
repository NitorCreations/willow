package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_CLASSPATH;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_JAR;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_MAIN_CLASS;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_ARGS;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_JAVA_ARGS;

import java.io.File;

import com.nitorcreations.willow.utils.MergeableProperties;

public class JavaLauncher extends AbstractLauncher {
	protected String mainClass="";
	protected File launchJar=null;
	protected String classPath="";


	@Override
	public Integer call() {
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
		return super.call();
	}

	@Override
	public void setProperties(MergeableProperties properties, String keyPrefix) {
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
