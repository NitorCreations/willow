package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_ARGS;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_CLASSPATH;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_JAR;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_JAVA_ARGS;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_MAIN_CLASS;

import java.io.File;

import javax.inject.Named;

import com.nitorcreations.willow.utils.MergeableProperties;

@Named
public class JavaLauncher extends AbstractLauncher {
  protected String mainClass = "";
  protected File launchJar = null;
  protected String classPath = "";

  @Override
  public Integer call() {
    if (!mainClass.isEmpty() && !classPath.isEmpty()) {
      launchArgs.add("-cp");
      launchArgs.add(classPath);
      launchArgs.add(mainClass);
    } else {
      launchArgs.add("-jar");
      launchArgs.add(launchJar.getPath());
    }
    addLauncherArgs(launchProperties, PROPERTY_KEY_SUFFIX_ARGS);
    return super.call();
  }

  @Override
  public void setProperties(MergeableProperties properties, LaunchCallback callback) {
    super.setProperties(properties, callback);
    mainClass = properties.getProperty(PROPERTY_KEY_SUFFIX_MAIN_CLASS, "");
    classPath = properties.getProperty(PROPERTY_KEY_SUFFIX_CLASSPATH, "");
    String jarPath = properties.getProperty(PROPERTY_KEY_SUFFIX_JAR);
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
    addLauncherArgs(properties, PROPERTY_KEY_SUFFIX_JAVA_ARGS);
  }
}
