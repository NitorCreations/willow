package com.nitorcreations.willow.servers;


import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.google.inject.Injector;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Singleton
@SuppressFBWarnings(value={"SE_TRANSIENT_FIELD_NOT_RESTORED"}, justification="port set from init")
public class BackendWebsocketServlet extends WebSocketServlet {
  private static final long serialVersionUID = 4980353154914279832L;

  @Inject
  private transient Injector injector;
  private transient String port = "5122";
  @Override
  public void init() throws ServletException {
    super.init();
    ServletConfig config = getServletConfig();
    port = config.getInitParameter("backend.port");
    if (port == null || port.isEmpty()) port = "5122";
  }
  @Override
  public void configure(WebSocketServletFactory factory) {
    factory.getPolicy().setIdleTimeout(TimeUnit.MINUTES.toMillis(10));
    factory.getPolicy().setMaxTextMessageBufferSize(1024 * 1024);
    factory.getPolicy().setMaxTextMessageSize(1024 * 1024 * 5);
    BackendProxySocket.setPort(port);
    factory.setCreator(new WebSocketCreator() {
      @Override
      public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
        return injector.getInstance(BackendProxySocket.class);
      }
    });
  }
}
