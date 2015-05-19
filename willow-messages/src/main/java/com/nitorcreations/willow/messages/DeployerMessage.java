package com.nitorcreations.willow.messages;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value={"EI_EXPOSE_REP2"}, 
  justification="Just a simple encoding class and we want to avoid unnecessary memory copying")
public class DeployerMessage {
  public int type;
  public byte[] message;
  public DeployerMessage() {}
  public DeployerMessage(int type, byte[] message) {
    this.type = type;
    this.message = message;
  }
}
