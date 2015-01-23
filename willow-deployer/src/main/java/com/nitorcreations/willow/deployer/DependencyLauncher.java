package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_ARTIFACT;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_LAUNCH;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_RESOLVE_TRANSITIVE;

import java.io.File;

import com.nitorcreations.willow.utils.MergeableProperties;

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
  public void setProperties(MergeableProperties properties) {
    this.setProperties(properties, PROPERTY_KEY_PREFIX_LAUNCH);
  }

  @Override
  public void setProperties(MergeableProperties properties, String keyPrefix) {
    super.setProperties(properties);
    downloader = new AetherDownloader();
    downloader.setProperties(properties);
    artifactCoords = properties.getProperty(PROPERTY_KEY_ARTIFACT);
    transitive = Boolean.valueOf(properties.getProperty(PROPERTY_KEY_RESOLVE_TRANSITIVE, "false"));
  }
}
