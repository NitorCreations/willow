package com.nitorcreations.willow.deployer.launch;

public interface LaunchCallback {
  public boolean autoRestartDefault();
  public void postStart() throws Exception;
  public void postStop() throws Exception;
  
}
