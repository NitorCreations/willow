package com.nitorcreations.willow.ssh;


import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jetty.websocket.api.Session;

/**
 * class to send output to web socket client
 */
public class RawSentOutputTask implements Runnable {
  Session session;
  InputStream output;
  private final int BUFFER_LEN = 8 * 1024;

  public RawSentOutputTask(Session session, InputStream output) {
    this.session = session;
    this.output = output;
  }

  public void run() {
    byte[] buf = new byte[BUFFER_LEN ];
    while (session.isOpen()) {
      int read;
      try {
        while ((read = output.read(buf)) != -1) {
          try {
            if (read > 0) {
              this.session.getRemote().sendString(new String(buf, 0, read, "UTF-8"));            }
          } catch (IOException e) {
            e.printStackTrace();
          }
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
