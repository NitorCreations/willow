package com.nitorcreations.willow.ssh;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.Session;

/**
 * class to send output to web socket client
 */
public class RawSentOutputTask implements Runnable {
  private Logger log = Logger.getLogger(getClass().getCanonicalName());
  Session session;
  InputStream output;
  private static final int BUFFER_LEN = 8 * 1024;

  public RawSentOutputTask(Session session, InputStream output) {
    this.session = session;
    this.output = output;
  }

  public void run() {
    byte[] buf = new byte[BUFFER_LEN];
    while (session.isOpen()) {
      int read;
      try {
        while ((read = output.read(buf)) != -1) {
          try {
            if (read > 0) {
              this.session.getRemote().sendString(new String(buf, 0, read, StandardCharsets.UTF_8));            }
          } catch (IOException e) {
            log.log(Level.INFO, "Failed to send read data from ssh to client", e);
          }
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            log.log(Level.INFO, "Interrupted while waiting for data", e);
          }
        }
      } catch (IOException e) {
        log.log(Level.INFO, "Failed to read data from ssh", e);
      }
    }
  }
}
