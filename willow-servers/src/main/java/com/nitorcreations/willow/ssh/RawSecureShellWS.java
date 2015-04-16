package com.nitorcreations.willow.ssh;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.google.gson.Gson;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;

@WebSocket
public class RawSecureShellWS {
  private Logger log = Logger.getLogger(getClass().getCanonicalName());
  private Session session;
  private ChannelShell shell;
  private JSch jsch = new JSch();
  private com.jcraft.jsch.Session jschSession;
  private CountDownLatch closeLatch;
  private PrintStream inputToShell;
  public static final int BUFFER_LEN = 4 * 1024;

  public RawSecureShellWS() {
    this.closeLatch = new CountDownLatch(1);
  }

  public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
    return this.closeLatch.await(duration, unit);
  }

  @OnWebSocketConnect
  public void onConnect(Session session) {
    System.out.printf("Got connect: %s%n", session);
    session.setIdleTimeout(TimeUnit.MINUTES.toMillis(1));
    this.session = session;
    Connector con = null;
    try {
      ConnectorFactory cf = ConnectorFactory.getDefault();
      con = cf.createConnector();
    } catch (AgentProxyException e) {
      System.out.println(e);
    }
    if (con != null) {
      IdentityRepository irepo = new RemoteIdentityRepository(con);
      jsch.setIdentityRepository(irepo);
    }
    Map<String, List<String>> parameterMap = session.getUpgradeRequest().getParameterMap();
    String host = getStringParameter(parameterMap, "host", null);
    String user = getStringParameter(parameterMap, "user", null);
    Resize resize = new Resize();
    resize.cols = getIntParameter(parameterMap, "cols", 80);
    resize.rows = getIntParameter(parameterMap, "rows", 24);
    try {
      java.util.Properties config = new java.util.Properties();
      config.put("StrictHostKeyChecking", "no");
      jschSession = jsch.getSession(user, host, 22);
      jschSession.setConfig(config);
      jschSession.connect(60000);
      shell = (ChannelShell) jschSession.openChannel("shell");
      shell.setAgentForwarding(true);
      shell.setPtyType("vt102");
      shell.connect();
      shell.setPtySize(resize.cols, resize.rows, resize.getPixelWidth(), resize.getPixelHeight());
    } catch (JSchException e) {
      close(1, "Failed to create ssh session", e);
    }
    Runnable run;
    try {
      run = new RawSentOutputTask(session, new BufferedInputStream(shell.getInputStream(), BUFFER_LEN));
      Thread thread = new Thread(run);
      thread.start();
    } catch (IOException e) {
      close(2, "IOException while getting data from ssh", e);
    }
    try {
      inputToShell = new PrintStream(shell.getOutputStream(), true);
    } catch (IOException e) {
      close(3, "IOException while creating write stream to ssh", e);
    }
  }
  @OnWebSocketMessage
  public void onMessage(String message) {
    if (session.isOpen()) {
      if (message != null && !message.isEmpty()) {
        if (message.startsWith("{\"cols\":")) {
          Resize resize = new Gson().fromJson(message, Resize.class);
          shell.setPtySize(resize.cols, resize.rows, resize.getPixelWidth(), resize.getPixelHeight());
        } else if (message.startsWith("{\"ping\":")) {
          if (!shell.isConnected()) {
             try {
              session.getRemote().sendPing(ByteBuffer.wrap(message.getBytes()));
            } catch (IOException e) {
              close(4, "IOException while sending ping data to client", e);
            }
          }
        } else {
          try {
            inputToShell.write(message.getBytes(Charset.forName("UTF-8")));
          } catch (IOException e) {
            close(5, "IOException while sending data to ssh", e);
          }
        }
      }
    }
  }

  public void close(int statusCode, String reason, Exception e) {
    log.log(Level.INFO, "Caught execption: "+ reason, e);
    session.close();
    onClose(1, reason);
  }
  @OnWebSocketClose
  public void onClose(int statusCode, String reason) {
    log.log(Level.INFO, "Connection closed: " + statusCode + "-" + reason);
    if (shell != null) {
      shell.disconnect();
    }
    if (jschSession != null) {
      jschSession.disconnect();
    }
    this.session = null;
    this.closeLatch.countDown();
  }
  private int getIntParameter(Map<String, List<String>> parameterMap, String name, int def) {
    List<String> vals = parameterMap.get(name);
    if (vals == null || vals.isEmpty()) return def;
    try {
      return Integer.parseInt(vals.get(0));
    } catch (NumberFormatException e) {
      return def;
    }
  }
  private String getStringParameter(Map<String, List<String>> parameterMap, String name, String def) {
    List<String> vals = parameterMap.get(name);
    if (vals == null || vals.isEmpty()) return def;
    return vals.get(0);
  }
  private static class Resize {
    public int cols=80;
    public int rows=24;
    public int getPixelWidth() {
      return cols * 12;
    }
    public int getPixelHeight() {
      return cols * 16;
    }
  }

}
