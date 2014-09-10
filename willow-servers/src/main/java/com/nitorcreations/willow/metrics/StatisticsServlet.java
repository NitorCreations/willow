package com.nitorcreations.willow.metrics;

import java.util.concurrent.TimeUnit;

import javax.servlet.annotation.WebServlet;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

@WebServlet(name = "Statistics WebSocket Servlet", urlPatterns = { "/statistics" })
public class StatisticsServlet extends WebSocketServlet {
	private static final long serialVersionUID = 4980353154914279832L;

	@Override
	public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(TimeUnit.MINUTES.toMillis(10));
        factory.register(SaveEventsSocket.class);
	}

}
