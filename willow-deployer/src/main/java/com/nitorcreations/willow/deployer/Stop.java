package com.nitorcreations.willow.deployer;

import com.google.inject.Singleton;

@Singleton
public class Stop extends DeployerControl {
  public static void main(String[] args) {
    injector.getInstance(Stop.class).stopOld(args);
  }
}
