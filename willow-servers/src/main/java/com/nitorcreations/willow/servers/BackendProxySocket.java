package com.nitorcreations.willow.servers;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import javax.inject.Named;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;


@WebSocket
@Named
public class BackendProxySocket extends BasicWillowSocket {
  private transient static AtomicReference<String> port = new AtomicReference<String>("5122");
  public static void setPort(String port) {
    BackendProxySocket.port.set(port);
  }
  private transient WebSocketClient client;
  private transient URI uri;
  private Session wsSession;
  private Future<Session> connectFuture;
  @Override
  @OnWebSocketConnect
  public void onConnect(Session session) {
    super.onConnect(session);
    this.session.setIdleTimeout(TimeUnit.MINUTES.toMillis(1));
    StringBuilder destUri = new StringBuilder();
    URI requestUri = session.getUpgradeRequest().getRequestURI();
    destUri.append("ws://localhost:").append(port.get()).append(requestUri.getPath());
    String query = requestUri.getQuery();
    if (query != null && !query.isEmpty()) {
      destUri.append("?").append(query);
    }
    try {
      uri = new URI(destUri.toString());
      connect();
    } catch (Exception e) {
      log.log(Level.INFO, "Exception while trying to connect", e);
    }
  }
  private void connect() throws Exception {
    wsSession = null;
    client = new WebSocketClient();
    client.start();
    client.setAsyncWriteTimeout(5000);
    client.setConnectTimeout(2000);
    client.setStopTimeout(5000);
    ClientUpgradeRequest request = new ClientUpgradeRequest();
    connectFuture = client.connect(new ClientWebsocket(), uri, request);
    log.info(String.format("Connecting to : %s", uri));
    try {
      wsSession = connectFuture.get();
    } catch (Exception e) {
      log.log(Level.INFO, "Exception while trying to connect", e);
      try {
        client.stop();
        client.destroy();
      } catch (Exception e1) {
        log.log(Level.INFO, "Exception while trying to disconnect", e1);
      }
    }
  }
  @OnWebSocketMessage
  public void onText(Session session, String message) throws IOException
  {
    if (!session.isOpen())
    {
      log.warning("Session is closed");
      return;
    }
    if (wsSession == null) {
      try {
        while (connectFuture == null) {
          Thread.sleep(100);
        }
        wsSession = connectFuture.get();
      } catch (InterruptedException | ExecutionException e) {
        log.log(Level.WARNING, "Failed to connect to backend websocket", e);
      }
    }
    if (wsSession != null && wsSession.isOpen()) {
      try {
        wsSession.getRemote().sendString(message);
      } catch (IOException e) {
        log.log(Level.INFO, "Exception while send to backend", e);
      }            
    }
  }
  @OnWebSocketMessage
  public void onBinary(Session session, byte buf[], int offset, int length) throws IOException {
    if (!session.isOpen())  {
      log.warning("Session is closed");
      return;
    }
    if (wsSession.isOpen()) {
      try {
        wsSession.getRemote().sendBytes(ByteBuffer.wrap(buf, offset, length));
      } catch (IOException e) {
        log.log(Level.INFO, "Exception while send to backend", e);
      }            
    }
  }
  @OnWebSocketClose
  public void onClose(int statusCode, String reason) {
    super.onClose(statusCode, reason);
  }

  @WebSocket
  public class ClientWebsocket {
    @OnWebSocketMessage
    public void onText(String message) {
      if (session.isOpen()) {
        try {
          session.getRemote().sendString(message);
        } catch (IOException e) {
          log.log(Level.INFO, "Exception while send from backend", e);
        }            
      }
    }
    @OnWebSocketMessage
    public void onBinary(byte buf[], int offset, int length) {
      if (session.isOpen()) {
        try {
          session.getRemote().sendBytes(ByteBuffer.wrap(buf, offset, length));
        } catch (IOException e) {
          log.log(Level.INFO, "Exception while send from backend", e);
        }
      }
    }
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
      log.log(Level.INFO, "Backend websocket closed - " + reason + "(" + statusCode + ")");
      if (session.isOpen()) {
        session.close();
      }      
    }
  }
}
