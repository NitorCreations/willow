package com.nitorcreations.willow.servers;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.elasticsearch.node.Node;

import com.google.gson.Gson;
import com.nitorcreations.willow.metrics.Metric;

@WebSocket
@Named
public class ServerSidePollingSocket extends BasicWillowSocket {
  @Inject
  private Node node;

  private final Map<String, Metric> metrics;
  
  @Inject
  public ServerSidePollingSocket(Map<String, Metric> metrics) {
    this.metrics = new HashMap<>(metrics);
  }

  @OnWebSocketMessage
  public void messageReceived(String message) {
    try {
      Gson gson = new Gson();
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
