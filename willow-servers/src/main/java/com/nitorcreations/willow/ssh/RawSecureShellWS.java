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

import javax.inject.Inject;

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
import com.nitorcreations.willow.servers.BasicWillowSocket;
import com.nitorcreations.willow.servers.HostLookupService;
import com.sun.jna.platform.win32.WinUser.HOOKPROC;

@WebSocket
public class RawSecureShellWS extends BasicWillowSocket{
  private ChannelShell shell;
  private JSch jsch = new JSch();
  private com.jcraft.jsch.Session jschSession;
  private PrintStream inputToShell;
  public static final int BUFFER_LEN = 4 * 1024;
  @Inject
  HostLookupService hostLookupService;
  
  @OnWebSocketConnect
  public void onConnect(Session session) {
    super.onConnect(session);
    this.session.setIdleTimeout(TimeUnit.MINUTES.toMillis(1));
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
    String connectHost = hostLookupService.getResolvableHostname(host);
    String user = getStringParameter(parameterMap, "user", null);
    if ("@admin".equals(user)) {
      user = hostLookupService.getAdminUserFor(host);
    }
    Resize resize = new Resize();
    resize.cols = getIntParameter(parameterMap, "cols", 80);
    resize.rows = getIntParameter(parameterMap, "rows", 24);
    try {
      java.util.Properties config = new java.util.Properties();
      config.put("StrictHostKeyChecking", "no");
      jschSession = jsch.getSession(user, connectHost, hostLookupService.getSshPort(host));
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
    if (shell != null) {
      shell.disconnect();
    }
    if (jschSession != null) {
      jschSession.disconnect();
    }
    super.onClose(statusCode, reason);
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
