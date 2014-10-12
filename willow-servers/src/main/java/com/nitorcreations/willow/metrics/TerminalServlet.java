package com.nitorcreations.willow.metrics;

import java.util.concurrent.TimeUnit;

import javax.servlet.annotation.WebServlet;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.nitorcreations.willow.ssh.SecureShellWS;

public class TerminalServlet extends WebSocketServlet {
	private static final long serialVersionUID = -7940037116569261919L;

	@Override
	public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(TimeUnit.MINUTES.toMillis(1));
        factory.register(SecureShellWS.class);
	}

}
