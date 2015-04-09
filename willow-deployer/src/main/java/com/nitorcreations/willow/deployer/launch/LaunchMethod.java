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
  }
  public void setProperties(MergeableProperties properties);
  public void setProperties(MergeableProperties properties, LaunchCallback callback);
  public long getProcessId();
  public void stopRelaunching();
  public int destroyChild() throws InterruptedException;
  public int restarts();
  public String getName();
}
