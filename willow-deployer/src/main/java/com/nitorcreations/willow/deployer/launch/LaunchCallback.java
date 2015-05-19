package com.nitorcreations.willow.deployer.launch;

public interface LaunchCallback {
  boolean autoRestartDefault();
  void postStart() throws Exception;
  void postStop() throws Exception;
  
}
