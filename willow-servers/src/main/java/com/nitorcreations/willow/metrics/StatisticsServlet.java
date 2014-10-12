package com.nitorcreations.willow.metrics;

import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class StatisticsServlet extends WebSocketServlet {
	private static final long serialVersionUID = 4980353154914279832L;

	@Override
	public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(TimeUnit.MINUTES.toMillis(10));
        factory.register(SaveEventsSocket.class);
	}

}
