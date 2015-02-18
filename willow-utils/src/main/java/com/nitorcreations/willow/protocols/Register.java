package com.nitorcreations.willow.protocols;

public class Register {
  public static void doIt() {
    String oldPkgs = System.getProperty("java.protocol.handler.pkgs");
    String newPkgs = "com.nitorcreations.willow.protocols";
    if (oldPkgs != null && !oldPkgs.isEmpty()) {
      newPkgs += "|" + oldPkgs;
    }
    System.setProperty("java.protocol.handler.pkgs", newPkgs);
  }
}
