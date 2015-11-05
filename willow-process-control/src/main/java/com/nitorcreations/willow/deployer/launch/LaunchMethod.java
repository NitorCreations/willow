package com.nitorcreations.willow.deployer.launch;

import java.util.concurrent.Callable;

import com.nitorcreations.willow.utils.MergeableProperties;

public interface LaunchMethod extends Callable<Integer> {
  public enum TYPE {
    DEPENDENCY(DependencyLauncher.class), NATIVE(NativeLauncher.class), JAVA(JavaLauncher.class);
    Class<? extends LaunchMethod> launcher;

    private TYPE(Class<? extends LaunchMethod> launcher) {
      this.launcher = launcher;
    }

    public Class<? extends LaunchMethod> getLauncher() {
      return launcher;
    }
    public static TYPE fromString(String name) {
      try {
        return valueOf(name);
      } catch (IllegalArgumentException | NullPointerException e) {
        return null;
      }
    }
  }
  void setProperties(MergeableProperties properties);
  void setProperties(MergeableProperties properties, LaunchCallback callback);
  long getProcessId();
  void stopRelaunching();
  int destroyChild() throws InterruptedException;
  int restarts();
  String getName();
}
