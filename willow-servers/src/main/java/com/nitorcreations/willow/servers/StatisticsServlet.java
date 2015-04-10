package com.nitorcreations.willow.servers;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

@Singleton
public class StatisticsServlet extends WebSocketServlet {
  private static final long serialVersionUID = 4980353154914279832L;

  @Override
  public void configure(WebSocketServletFactory factory) {
    factory.getPolicy().setIdleTimeout(TimeUnit.MINUTES.toMillis(10));
    factory.getPolicy().setMaxBinaryMessageBufferSize(1024 * 1024);
    factory.getPolicy().setMaxBinaryMessageSize(1024 * 1024 * 5);
    factory.register(SaveEventsSocket.class);
  }
}
