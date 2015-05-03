package com.nitorcreations.willow.messages;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.jcraft.jsch.Identity;
import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;

public class WebSocketTransmitter {
  private long flushInterval = 2000;
  private URI uri;
  private String username;
  
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final ArrayBlockingQueue<AbstractMessage> queue = new ArrayBlockingQueue<AbstractMessage>(200);
  private final Worker worker = new Worker();
  private final Thread workerThread = new Thread(worker, "websocket-transfer");
  private final MessageMapping msgmap = new MessageMapping();
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
    if (workerThread.isAlive()) {
      worker.stop();
    }
  }
  public boolean isRunning() {
    return workerThread.isAlive();
  }

  public boolean queue(AbstractMessage msg) {
    logger.fine("Queue message type: " + MessageMapping.map(msg.getClass()));
    try {
      while (!queue.offer(msg, flushInterval * 2, TimeUnit.MILLISECONDS)) {
        logger.info("queue full, retrying");
      }
    } catch (InterruptedException e) {
      LogRecord rec = new LogRecord(Level.INFO, "Interrupted");
      rec.setThrown(e);
      logger.log(rec);
      return false;
    }
    return true;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @WebSocket
  public class Worker implements Runnable {
    private boolean running = true;
    private final ArrayList<AbstractMessage> send = new ArrayList<AbstractMessage>();
    private Session wsSession;
    private WebSocketClient client = new WebSocketClient();

    @Override
    public void run() {
      synchronized (this) {
        while (running) {
          try {
            try {
              if ((!client.isRunning() && !client.isStarting()) || client.isFailed()) {
                connect();
              }
            } catch (Exception e) {
              throw new RuntimeException("Failed to connect to " + uri.toString(), e);
            }
            try {
              this.wait(flushInterval);
              if (wsSession == null)
                continue;
              doSend();
            } catch (IOException e) {
              LogRecord rec = new LogRecord(Level.INFO, "Exception while sending messages");
              rec.setThrown(e);
              logger.log(rec);
            } catch (InterruptedException e) {
              LogRecord rec = new LogRecord(Level.INFO, "Interrupted");
              rec.setThrown(e);
              logger.log(rec);
              try {
                doSend();
              } catch (IOException e1) {
                LogRecord rec2 = new LogRecord(Level.INFO, "Exception while sending messages");
                rec2.setThrown(e);
                logger.log(rec2);
              }
              return;
            }
          } catch (Exception e) {
            LogRecord rec2 = new LogRecord(Level.INFO, "Exception while sending messages");
            rec2.setThrown(e);
            logger.log(rec2);
          }
        }
      }
    }

    private void doSend() throws IOException {
      queue.drainTo(send);
      if (send.size() > 0) {
        logger.fine(String.format("Sending %d messages", send.size()));
        ByteBuffer toSend = msgmap.encode(send);
        logger.fine(String.format("Sending buffer len %d", toSend.capacity()));
        wsSession.getRemote().sendBytes(toSend);
        send.clear();
      } else {
        wsSession.getRemote().sendPing(ByteBuffer.allocate(4).putInt(1234));
      }
    }

    public void stop() {
      synchronized (this) {
        this.running = false;
        this.notifyAll();
      }
      this.wsSession.close();
      try {
        client.stop();
      } catch (Exception e) {
        LogRecord rec = new LogRecord(Level.INFO, "Exception while trying to stop");
        rec.setThrown(e);
        logger.log(rec);
      }
      client.destroy();
    }

    private void connect() throws Exception {
      wsSession = null;
      client = new WebSocketClient();
      client.start();
      ClientUpgradeRequest request = new ClientUpgradeRequest();
      synchronized (this) {
        request.setHeader("Authorization", "PUBLICKEY " + getAuthorization());
        Future<Session> future = client.connect(this, uri, request);
        logger.info(String.format("Connecting to : %s", uri));
        try {
          wsSession = future.get();
        } catch (Exception e) {
          LogRecord rec = new LogRecord(Level.INFO, "Exception while trying to connect");
          rec.setThrown(e);
          logger.log(rec);
          try {
            client.stop();
            client.destroy();
          } catch (Exception e1) {
            LogRecord rec2 = new LogRecord(Level.INFO, "Exception while trying to disconnect");
            rec2.setThrown(e1);
            logger.log(rec2);
          }
        }
      }
    }
    private String getAuthorization() {
      StringBuilder ret = new StringBuilder();
      String now = "" + System.currentTimeMillis();
      Connector con = null;
      try {
        ConnectorFactory cf = ConnectorFactory.getDefault();
        con = cf.createConnector();
      } catch (AgentProxyException e) {
        System.out.println(e);
      }
      byte[] sign = (username + ":" + now).getBytes(StandardCharsets.UTF_8);
      ret.append(printBase64Binary(sign));
      if (con != null) {
        IdentityRepository irepo = new RemoteIdentityRepository(con);
        for (Identity id : (List<Identity>)irepo.getIdentities()) {
          byte[] sig = id.getSignature(sign);
          ret.append(" ").append(printBase64Binary(sig));
        }
      }
      return ret.toString();
    }
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
      logger.info(String.format("Connection closed: %d - %s", statusCode, reason));
      try {
        client.stop();
        client.destroy();
      } catch (Exception e) {
        LogRecord rec = new LogRecord(Level.INFO, "Exception while trying to handle socket close");
        rec.setThrown(e);
        logger.log(rec);
      }
    }

    @OnWebSocketError
    public void onError(Session wsSession, Throwable err) {
      logger.info(String.format("Connection error: %s - %s", wsSession, err.getMessage()));
      try {
        client.stop();
        client.destroy();
      } catch (Exception e) {
        LogRecord rec = new LogRecord(Level.INFO, "Exception while trying to handle socket error");
        rec.setThrown(e);
        logger.log(rec);
      }
    }
  }
}
