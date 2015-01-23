package com.nitorcreations.willow.deployer;

import java.io.InputStream;

import com.nitorcreations.willow.messages.OutputMessage;
import com.nitorcreations.willow.messages.WebSocketTransmitter;
import com.nitorcreations.willow.utils.AbstractStreamPumper;

class StreamLinePumper extends AbstractStreamPumper implements Runnable {
  private final WebSocketTransmitter transmitter;

  public StreamLinePumper(InputStream in, WebSocketTransmitter transmitter, String name) {
    super(in, name);
    this.transmitter = transmitter;
  }

  @Override
  public void handle(String line) {
    OutputMessage msg = new OutputMessage(name, line);
    transmitter.queue(msg);
  }
}
