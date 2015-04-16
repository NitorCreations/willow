package com.nitorcreations.willow.servers;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.nitorcreations.willow.ssh.RawSecureShellWS;

@Singleton
public class RawTerminalServlet extends WebSocketServlet {
  private static final long serialVersionUID = -7940037116569261919L;

  @Override
  public void configure(WebSocketServletFactory factory) {
    factory.getPolicy().setIdleTimeout(TimeUnit.MINUTES.toMillis(5));
    factory.getPolicy().setMaxBinaryMessageBufferSize(1024 * 1024);
    factory.getPolicy().setMaxBinaryMessageSize(1024 * 1024 * 5);
    factory.register(RawSecureShellWS.class);
  }
}
