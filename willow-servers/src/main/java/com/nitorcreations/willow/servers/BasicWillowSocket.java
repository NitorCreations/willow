package com.nitorcreations.willow.servers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.Session;

public class BasicWillowSocket {
  protected Logger log = Logger.getLogger(getClass().getCanonicalName());
  private final CountDownLatch closeLatch;
  protected Session session;

  public BasicWillowSocket() {
    this.closeLatch = new CountDownLatch(1);
  }
  public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
    return this.closeLatch.await(duration, unit);
  }
  public void onClose(int statusCode, String reason) {
    log.info("Connection closed: "+ statusCode + " - " + reason);
    this.session = null;
    this.closeLatch.countDown();
  }
  public void onConnect(Session session) {
    log.info("Got connect: " + session);
    this.session = session;
  }
}
