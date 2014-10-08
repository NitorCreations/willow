package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.*;

import java.io.File;
import java.util.Properties;


public class DependencyLauncher extends JavaLauncher implements LaunchMethod {
	String artifactCoords;
	private boolean transitive = false;
	private AetherDownloader downloader;
	
	@Override
	public Integer call() {
		if (transitive) {
			classPath = downloader.downloadTransitive(artifactCoords);
			launchJar = new File(classPath.split(File.pathSeparator)[0]);
		} else {
			launchJar = downloader.downloadArtifact(artifactCoords);
			classPath = launchJar.getAbsolutePath();
		}
		return super.call();
	}
	@Override
	public void setProperties(Properties properties) {
		this.setProperties(properties, PROPERTY_KEY_PREFIX_LAUNCH);
	}
	@Override
	public void setProperties(Properties properties, String keyPrefix) {
		super.setProperties(properties);
		downloader = new AetherDownloader();
		downloader.setProperties(properties);
		artifactCoords = properties.getProperty(PROPERTY_KEY_ARTIFACT);
		transitive = Boolean.valueOf(properties.getProperty(PROPERTY_KEY_RESOLVE_TRANSITIVE, "false"));
	}
}
