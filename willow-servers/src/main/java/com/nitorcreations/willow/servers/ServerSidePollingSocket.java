package com.nitorcreations.willow.servers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;

import com.google.gson.Gson;
import com.nitorcreations.willow.messages.metrics.MetricConfig;
import com.nitorcreations.willow.metrics.Metric;

@WebSocket
@Named
public class ServerSidePollingSocket extends BasicWillowSocket {
  @Inject
  private Node node;
  @Inject
  ScheduledExecutorService scheduler;
  private final Map<String, Metric> metrics;
  private List<ScheduledFuture<?>> pollers = new ArrayList<>();
  public final int minStep = 1000;
  private final Gson gson = new Gson();
  private class PollTask implements Runnable {
    private final Metric metric;
    private final MetricConfig conf;
    private final long currTimeDelay;
    private final Session session;
    private final Gson gson = new Gson();
    public PollTask(Session session, MetricConfig conf) {
      this.conf = conf;
      this.metric = metrics.get(conf.getMetricKey());
      if (metric == null) throw new IllegalArgumentException("No metric found for " + conf.getMetricKey());
      long now = System.currentTimeMillis();
      this.currTimeDelay = now - conf.getStop();
      this.session = session;
    }

    @Override
    public void run() {
      try (Client client = node.client()){
        long stop = System.currentTimeMillis() - currTimeDelay;
        long start = stop - conf.getStep();
        Metric nextMetric = metric.getClass().newInstance();
        Object ret = nextMetric.calculateMetric(conf);
        conf.setStop(stop);
        conf.setStart(start);
        session.getRemote().sendString(gson.toJson(ret));
      } catch (InstantiationException | IllegalAccessException | IOException e) {
        log.log(Level.INFO, "Failed to send polled statistics", e);
      }
    }
  }
  @Inject
  public ServerSidePollingSocket(Map<String, Metric> metrics) {
    this.metrics = new HashMap<>(metrics);
  }

  @OnWebSocketMessage
  public void messageReceived(Session session, String message) {
    try {
      MetricConfig conf = gson.fromJson(message, MetricConfig.class);
      PollTask task = new PollTask(session, conf);
      ScheduledFuture<?> handle = scheduler.scheduleAtFixedRate(task, 0, conf.getStep(), TimeUnit.MILLISECONDS);
      pollers.add(handle);
    } catch (Exception e) {
      log.log(Level.INFO, "Failed to schedule task", e);
    }
  }
  @OnWebSocketConnect
  public void onConnect(Session session) {
    super.onConnect(session);
  }
  @OnWebSocketClose
  public void onClose(int statusCode, String reason) {
    for (ScheduledFuture<?> next : pollers) {
      next.cancel(true);
    }
    super.onClose(statusCode, reason);
  }

}
