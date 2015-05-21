package com.nitorcreations.willow.messages;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.nitorcreations.willow.utils.SSHAgentAuthorizationUtil;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.nitorcreations.willow.messages.event.Event;

public class WebSocketTransmitter {
  private long flushInterval = 2000;
  private URI uri;
  private String username;

  private static final Logger logger = Logger.getLogger(WebSocketTransmitter.class.getName());
  private final ArrayBlockingQueue<AbstractMessage> queue = new ArrayBlockingQueue<AbstractMessage>(200);
  private final Worker worker = new Worker();
  private final Thread workerThread = new Thread(worker, "websocket-transfer");
  private final MessageMapping msgmap = new MessageMapping();
  private AtomicBoolean running = new AtomicBoolean(true);
  private static final Map<String, WebSocketTransmitter> singletonTransmitters = Collections.synchronizedMap(new HashMap<String, WebSocketTransmitter>());

  public static synchronized WebSocketTransmitter getSingleton(long flushInterval, String uri) throws URISyntaxException {
    WebSocketTransmitter ret = singletonTransmitters.get(uri);
    if (ret == null) {
      ret = new WebSocketTransmitter();
      ret.setFlushInterval(flushInterval);
      ret.setUri(new URI(uri));
      singletonTransmitters.put(uri, ret);
    }
    return ret;
  }

  public WebSocketTransmitter() {
    this.username = System.getProperty("user.name", "willow");
  }
  public long getFlushInterval() {
    return flushInterval;
  }
  public void setFlushInterval(long flushInterval) {
    this.flushInterval = flushInterval;
  }
  public URI getUri() {
    return uri;
  }
  public void setUri(URI uri) {
    this.uri = uri;
    if (uri.getUserInfo() != null && uri.getUserInfo() != null) {
      this.username = uri.getUserInfo().split(":")[0];
    }
  }

  public void start() {
    if (!workerThread.isAlive()) {
      workerThread.start();
    }
  }

  public void stop() {
    logger.finest("Stopping the message transmitter");
    running.set(false);
    if (workerThread.isAlive()) {
      worker.stop();
    }
  }
  public boolean isRunning() {
    return workerThread.isAlive();
  }

  public boolean queue(AbstractMessage msg) {
    synchronized (this) {
      if (!running.get()) {
        return false;
      }
    }
    logger.fine("Queue message type: " + MessageMapping.map(msg.getClass()));
    try {
      while (!queue.offer(msg, flushInterval * 2, TimeUnit.MILLISECONDS)) {
        logger.info("queue full, retrying");
      }
    } catch (InterruptedException e) {
      logger.log(Level.INFO, "Interrupted", e);
      return false;
    }
    return true;
  }

  public boolean queue (Event event) {
    return this.queue(event.getEventMessage());
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @WebSocket
  public class Worker implements Runnable {
    private AtomicBoolean running = new AtomicBoolean(true);
    private final ArrayList<AbstractMessage> send = new ArrayList<AbstractMessage>();
    private Session wsSession;
    private WebSocketClient client = new WebSocketClient();

    @Override
    public void run() {
      while (running.get() && !Thread.currentThread().isInterrupted()) {
        try {
          try {
            if (!client.isRunning() && !client.isStarting() || client.isFailed()) {
              connect();
            }
          } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to connect to " + uri.toString(), e);
            continue;
          }
          try {
            synchronized (this) {
              if (running.get() && !Thread.currentThread().isInterrupted()) {
                this.wait(flushInterval);
              }
            }
            if (wsSession == null)
              continue;
            doSend();
          } catch (IOException e) {
            logger.log(Level.INFO, "Exception while sending messages", e);
          } catch (InterruptedException e) {
            logger.log(Level.INFO, "Interrupted", e);
            try {
              doSend();
            } catch (IOException e1) {
              logger.log(Level.INFO, "Exception while sending messages", e1);
            }
            return;
          }
        } catch (Exception e) {
          logger.log(Level.INFO, "Exception while sending messages", e);
        }
      }

    }

    private void doSend() throws IOException {
      queue.drainTo(send);
      if (send.size() > 0) {
        logger.finest(String.format("Sending %d messages", send.size()));
        ByteBuffer toSend = msgmap.encode(send);
        logger.finest(String.format("Sending buffer len %d", toSend.capacity()));
        wsSession.getRemote().sendBytes(toSend);
        send.clear();
      } else {
        wsSession.getRemote().sendPing(ByteBuffer.allocate(4).putInt(1234));
      }
    }

    public void stop() {
      synchronized (this) {
        this.running.set(false);
        this.notifyAll();
      }
      try {
        workerThread.join();
      } catch (InterruptedException e) {
        logger.warning("Interrupted while waiting for transmitter to finish.");
      }

      try {
        client.stop();
      } catch (Exception e) {
        logger.log(Level.INFO, "Exception while trying to stop", e);
      }
      client.destroy();
    }

    private void connect() throws Exception {
      wsSession = null;
      client = new WebSocketClient();
      client.start();
      client.setAsyncWriteTimeout(5000);
      client.setConnectTimeout(2000);
      client.setStopTimeout(5000);
      ClientUpgradeRequest request = new ClientUpgradeRequest();
      request.setHeader("Authorization", SSHAgentAuthorizationUtil.getSshAgentAuthorization(username));
      Future<Session> future = client.connect(this, uri, request);
      logger.info(String.format("Connecting to : %s", uri));
      try {
        wsSession = future.get();
      } catch (Exception e) {
        logger.log(Level.INFO, "Exception while trying to connect", e);
        try {
          client.stop();
          client.destroy();
        } catch (Exception e1) {
          logger.log(Level.INFO, "Exception while trying to disconnect", e1);
        }
      }
    }
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
      logger.info(String.format("Connection closed: %d - %s", statusCode, reason));
      try {
        if (client.isRunning()) {
          client.stop();
          client.destroy();
        }
      } catch (Exception e) {
        logger.log(Level.INFO, "Exception while trying to handle socket close", e);
      }
    }

    @OnWebSocketError
    public void onError(Session wsSession, Throwable err) {
      logger.info(String.format("Connection error: %s - %s", wsSession, err.getMessage()));
      try {
        if (client.isRunning()) {
          client.stop();
          client.destroy();
        }
      } catch (Exception e) {
        logger.log(Level.INFO, "Exception while trying to handle socket error", e);
      }
    }
  }
}
