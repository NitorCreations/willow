package com.nitorcreations.willow.servers;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.google.inject.Injector;

@Singleton
public class ServerSidePollingServlet extends WebSocketServlet {
  private static final long serialVersionUID = 4980353154914279832L;

  @Inject
  Injector injector;
  
  @Override
  public void configure(WebSocketServletFactory factory) {
    factory.getPolicy().setIdleTimeout(TimeUnit.MINUTES.toMillis(10));
    factory.getPolicy().setMaxTextMessageBufferSize(1024 * 1024);
    factory.getPolicy().setMaxTextMessageSize(1024 * 1024 * 5);
    factory.setCreator(new WebSocketCreator() {
      @Override
      public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
        return injector.getInstance(ServerSidePollingSocket.class);
      }
    });
  }
}
