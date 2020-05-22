//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package com.nitorcreations.willow.servers.test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.nitorcreations.willow.utils.FileUtil;


/**
 * Example server using WebSocket and core Jetty Handlers
 */
public class ExampleEchoServer {
  static String willowPort = System.getProperty("willow.port", "5120");
  static String basicAuthHeader = "Basic " + FileUtil.printBase64Binary("admin:admin".getBytes(StandardCharsets.UTF_8));

  public static final class EchoSocketHandler extends WebSocketHandler {
    @Override
    public void configure(WebSocketServletFactory factory) {
      factory.setCreator(new EchoCreator());
    }
  }

  private static final Logger LOG = Log.getLogger(ExampleEchoServer.class);

  public static void main(String... args)  {
    try {
      boolean verbose = false;
      String docroot = "src/test/resources/";
      int port = 5122;
      for (int i = 0; i < args.length; i++) {
        String a = args[i];
        if ("-p".equals(a) || "--port".equals(a))
        {
          port = Integer.parseInt(args[++i]);
        }
        else if ("-v".equals(a) || "--verbose".equals(a))
        {
          verbose = true;
        }
        else if ("-d".equals(a) || "--docroot".equals(a))
        {
          docroot = args[++i];
        }
        else if (a.startsWith("-"))
        {
          usage();
        }
      }

      ExampleEchoServer server = new ExampleEchoServer(port);
      server.setVerbose(verbose);
      server.setResourceBase(docroot);
      server.runForever();
    }
    catch (Exception e) {
      LOG.warn(e);
      System.out.flush();
      System.exit(1);
    }
  }

  private static void usage() {
    System.err.println("java -cp{CLASSPATH} " + ExampleEchoServer.class + " [ OPTIONS ]");
    System.err.println("  -p|--port PORT    (default 8080)");
    System.err.println("  -v|--verbose ");
    System.err.println("  -d|--docroot file (default 'src/test/webapp')");
    System.exit(1);
  }

  private static Server server;

  private ServerConnector connector;
  private boolean _verbose;
  private static WebSocketHandler wsHandler;
  private static ResourceHandler rHandler;
  private static WebSocketClient client;

  public ExampleEchoServer(int port) {
    server = new Server();
    connector = new ServerConnector(server);
    connector.setPort(port);

    server.addConnector(connector);
    wsHandler = new EchoSocketHandler();

    server.setHandler(wsHandler);

    rHandler = new ResourceHandler();
    rHandler.setDirectoriesListed(true);
    rHandler.setResourceBase("src/test/webapp");
    wsHandler.setHandler(rHandler);
  }

  public String getResourceBase() {
    return rHandler.getResourceBase();
  }

  public boolean isVerbose() {
    return _verbose;
  }

  public void runForever() throws Exception {
    server.start();
    String host = connector.getHost();
    if (host == null) {
      host = "localhost";
    }
    int port = connector.getLocalPort();
    System.err.printf("Echo Server started on ws://%s:%d/%n",host,port);
    System.err.println(server.dump());
    HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + willowPort + "/ui/test.txt").openConnection();
    conn.addRequestProperty("Authorization", basicAuthHeader);
    try (InputStream in = conn.getInputStream()) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      FileUtil.copy(in, out);
      String res = out.toString("UTF-8");
      if (!res.startsWith("SUCCESS")) {
        try {
          server.stop();
        } finally {
          System.exit(1);
        }
      }
    }
    sendMessage();
  }

  public void setResourceBase(String dir) {
    rHandler.setResourceBase(dir);
  }

  public void setVerbose(boolean verbose) {
    _verbose = verbose;
  }
  private void sendMessage() {
    client = new WebSocketClient();
    SimpleEchoSocket socket = new SimpleEchoSocket();
    try {
      client.start();
      URI echoUri = new URI("ws://localhost:"+ willowPort + "/uiws/test");
      ClientUpgradeRequest request = new ClientUpgradeRequest();
      request.setHeader("Authorization", basicAuthHeader);
      client.connect(socket, echoUri, request).get();
      System.out.printf("Connecting to : %s%n", echoUri);
      socket.awaitClose(5, TimeUnit.SECONDS);
    } catch (Throwable t) {
      t.printStackTrace();
    } finally {
      try {
        client.stop();
        server.stop();
        if (socket.messageReceived) {
          System.exit(0);
        } else {
          System.exit(2);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }
  @WebSocket(maxTextMessageSize = 64 * 1024)
  public static class SimpleEchoSocket {

    public boolean messageReceived=false;

    private final CountDownLatch closeLatch;

    @SuppressWarnings("unused")
    @SuppressFBWarnings("URF_UNREAD_FIELD")
    private Session session;

    public SimpleEchoSocket() {
      this.closeLatch = new CountDownLatch(1);
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
      return this.closeLatch.await(duration, unit);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
      System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
      this.session = null;
      this.closeLatch.countDown();
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
      System.out.printf("Got connect: %s%n", session);
      this.session = session;
      try {
        Future<Void> fut;
        fut = session.getRemote().sendStringByFuture("MYTEST");
        fut.get(2, TimeUnit.SECONDS);
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }
    @OnWebSocketMessage
    public void onMessage(String msg) throws Exception {
      System.out.printf("Got msg: %s%n", msg);
      if ("MYTEST".equals(msg)) {
        messageReceived = true;
      }
    }
  }
}
