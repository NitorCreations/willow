package com.nitorcreations.willow.servers;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.google.inject.Injector;
import com.nitorcreations.willow.auth.Permissions;
import com.nitorcreations.willow.ssh.RawSecureShellWS;

@Singleton
public class RawTerminalServlet extends WebSocketServlet {
  private static final long serialVersionUID = -7940037116569261919L;

  @Inject
  private transient Injector injector;

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!SecurityUtils.getSubject().isPermitted(Permissions.ADMIN)) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
    super.service(req, resp);
  };
  @Override
  public void configure(WebSocketServletFactory factory) {
    factory.getPolicy().setIdleTimeout(TimeUnit.MINUTES.toMillis(5));
    factory.getPolicy().setMaxBinaryMessageBufferSize(1024 * 1024);
    factory.getPolicy().setMaxBinaryMessageSize(1024 * 1024 * 5);
    factory.setCreator(new WebSocketCreator() {
      @Override
      public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
        return injector.getInstance(RawSecureShellWS.class);
      }
    });
  }
}
