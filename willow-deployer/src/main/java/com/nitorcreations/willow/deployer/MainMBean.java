package com.nitorcreations.willow.deployer;

public interface MainMBean {
  public void stop();
  public String getStatus();
  public String[] getChildNames();
  public long getChildPid(String childName);
  public long getFirstJavaChildPid(String childName);
  public void restartChild(String childName);
}
