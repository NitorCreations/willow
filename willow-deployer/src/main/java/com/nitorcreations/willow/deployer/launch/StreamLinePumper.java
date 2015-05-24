package com.nitorcreations.willow.deployer.launch;

import java.io.InputStream;
import java.nio.charset.Charset;

import com.nitorcreations.willow.messages.LogMessage;
import com.nitorcreations.willow.messages.WebSocketTransmitter;
import com.nitorcreations.willow.utils.AbstractStreamPumper;

class StreamLinePumper extends AbstractStreamPumper {
  private final WebSocketTransmitter transmitter;

  public StreamLinePumper(InputStream in, WebSocketTransmitter transmitter, String name, Charset charset) {
    super(in, name, charset);
    this.transmitter = transmitter;
  }

  @Override
  public void handle(String line) {
    LogMessage msg = new LogMessage(System.currentTimeMillis(), getName(), line);
    transmitter.queue(msg);
  }
}
