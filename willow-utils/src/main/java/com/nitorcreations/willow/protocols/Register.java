package com.nitorcreations.willow.protocols;

public class Register {
  public static void doIt() {
    String oldPkgs = System.getProperty("java.protocol.handler.pkgs");
    String newPkgs = "com.nitorcreations.willow.protocols";
    if (oldPkgs != null && !oldPkgs.isEmpty()) {
      if (!oldPkgs.contains(newPkgs)) {
        newPkgs += "|" + oldPkgs;
      } else {
        newPkgs = oldPkgs;
      }
    }
    System.setProperty("java.protocol.handler.pkgs", newPkgs);
  }
}
