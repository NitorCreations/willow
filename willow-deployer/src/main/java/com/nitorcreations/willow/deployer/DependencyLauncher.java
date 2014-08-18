package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_LAUNCH_ARTIFACT;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_RESOLVE_TRANSITIVE;

import java.io.File;
import java.util.Properties;


public class DependencyLauncher extends JavaLauncher implements LaunchMethod {
	String artifactCoords;
	private boolean transitive = false;
	private AetherDownloader downloader;
	
	@Override
	public void run() {
		if (transitive) {
			classPath = downloader.downloadTransitive(artifactCoords);
			launchJar = new File(classPath.split(File.pathSeparator)[0]);
		} else {
			launchJar = downloader.downloadArtifact(artifactCoords);
			classPath = launchJar.getAbsolutePath();
		}
		super.run();
	}

	@Override
	public void setProperties(Properties properties) {
		super.setProperties(properties);
		downloader = new AetherDownloader();
		downloader.setProperties(properties);
		artifactCoords = properties.getProperty(PROPERTY_KEY_LAUNCH_ARTIFACT);
		transitive = Boolean.valueOf(properties.getProperty(PROPERTY_KEY_RESOLVE_TRANSITIVE, "false"));
	}
}
