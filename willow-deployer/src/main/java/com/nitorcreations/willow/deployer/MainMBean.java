package com.nitorcreations.willow.deployer;

public interface MainMBean {
  void stop();
  String getStatus();
  String[] getChildNames();
  long getChildPid(String childName);
  long getFirstJavaChildPid(String childName);
  void restartChild(String childName);
}
