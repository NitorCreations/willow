package com.nitorcreations.willow.deployer.launch;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.nitorcreations.willow.messages.LogMessage;
import com.nitorcreations.willow.messages.WebSocketTransmitter;
import com.nitorcreations.willow.utils.AbstractStreamPumper;

class StreamLinePumper extends AbstractStreamPumper {
  private final WebSocketTransmitter transmitter;
  private final Logger log;
  public StreamLinePumper(InputStream in, WebSocketTransmitter transmitter, String name, Charset charset) {
    super(in, name, charset);
    log = Logger.getLogger(name);
    this.transmitter = transmitter;
  }

  @Override
  public void handle(String line) {
    LogRecord rec = new LogRecord(Level.INFO, line);
    LogMessage msg = new LogMessage(rec);
    log.log(rec);
    transmitter.queue(msg);
  }
}
