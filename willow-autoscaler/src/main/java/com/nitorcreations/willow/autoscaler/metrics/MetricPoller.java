package com.nitorcreations.willow.autoscaler.metrics;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nitorcreations.willow.autoscaler.config.AutoScalingGroupConfig;
import com.nitorcreations.willow.autoscaler.config.AutoScalingPolicy;
import com.nitorcreations.willow.messages.metrics.MetricConfig;
import com.nitorcreations.willow.messages.metrics.TimePoint;
import com.nitorcreations.willow.sshagentauth.SSHUtil;

public class MetricPoller {

  private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

  @Inject
  private Gson gson;
  @Inject
  private ExecutorService executorService;
  @Inject
  private AutoScalingStatus autoScalingStatus;

  Map<AutoScalingGroupConfig, Map<String, GroupMetricListener>> groupMetricListeners;

  public void initialize(List<AutoScalingGroupConfig> groups, URI uri) {
    groupMetricListeners = new HashMap<>();
    for (AutoScalingGroupConfig group : groups) {
      Map<String, GroupMetricListener> groupListeners = groupMetricListeners.get(group);
      if (groupListeners == null) {
        groupListeners = new HashMap<>();
        groupMetricListeners.put(group, groupListeners);
      }
      for (AutoScalingPolicy policy : group.getScalingPolicies()) {
        String metricName = policy.getMetricName();
        if (!groupListeners.containsKey(metricName)) {
          GroupMetricListener listener = new GroupMetricListener(group, metricName, uri);
          groupListeners.put(metricName, listener);
          Future<?> future = executorService.submit(listener);
          if (!future.isDone()) {
            logger.fine("Started metrics listener for " + listener.metricName);
          }
        }
      }
    }
  }

  @WebSocket
  public class GroupMetricListener implements Runnable {

    private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

    private AutoScalingGroupConfig group;
    private String metricName;
    private URI uri;
    private WebSocketClient client;
    private AtomicBoolean running = new AtomicBoolean(true);
    private CountDownLatch connectLatch = new CountDownLatch(1);

    private GroupMetricListener(AutoScalingGroupConfig group, String metricName, URI uri) {
      this.group = group;
      this.metricName = metricName;
      this.uri = uri;
      createClient();
    }

    public synchronized WebSocketClient getClient() {
      if (this.client == null) {
        this.client = new WebSocketClient();
      }
      return this.client;
    }

    public synchronized void setClient(WebSocketClient client) {
      this.client = client;
    }

    public synchronized WebSocketClient createClient() {
      this.client = new WebSocketClient();
      return this.client;
    }

    public void connect() throws Exception {
      logger.info("Connecting metric listener for group " + group.getName() + ", metric " + metricName);
      WebSocketClient client = createClient();
      client.start();
      client.setAsyncWriteTimeout(5000);
      client.setConnectTimeout(2000);
      client.setStopTimeout(5000);
      ClientUpgradeRequest request = new ClientUpgradeRequest();
      request.setHeader(
          "Authorization",
          SSHUtil.getPublicKeyAuthorization(System.getProperty("user.name", "willow")));
      Future<Session> future = client.connect(this, uri, request);
      logger.info(String.format("Connecting to : %s", uri));
      try {
        future.get();
        logger.info(String.format("Connected to : %s", uri));
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Failed to connect metric poller with uri " + uri, e);
        if (client.isRunning()) {
          client.stop();
          client.destroy();
        }
        throw e;
      }
    }

    @OnWebSocketMessage
    public void messageReceived(Session session, String message) {
      try {
        if (message == null || message.isEmpty()) {
          return;
        }
        List<TimePoint<Double>> values = gson.fromJson(message, new TypeToken<List<TimePoint<Double>>>(){}.getType());
        if (values == null || values.isEmpty()) {
          return;
        }
        TimePoint<Double> metricValue = values.get(values.size() - 1);
        autoScalingStatus.addMetricValue(group.getName(), metricName, metricValue);
      } catch (Exception e) {
        logger.log(Level.INFO, "fail in receiving metric data", e);
      }
    }

    @OnWebSocketClose
    public void handleClose(Session closedSession, int statusCode, String reason) {
      logger.log(Level.INFO, "Metric listener websocket closed with status " + statusCode + ". Reason: " + reason);
      closeClient();
      connectLatch.countDown();
    }

    @OnWebSocketError
    public void handleError(Session errorSession, Throwable throwable) {
      logger.log(Level.INFO, "Metric listener websocket error", throwable);
      closeClient();
      connectLatch.countDown();
    }

    @OnWebSocketConnect
    public void startPolling(Session session) {
      logger.info("Sending poll request for " + this.metricName + " in group " + group.getName());
      long now = System.currentTimeMillis();
      MetricConfig metricConfig = new MetricConfig();
      metricConfig.setMetricKey(this.metricName);
      metricConfig.setStart(now - 20000);
      metricConfig.setStop(now - 10000);
      metricConfig.setStep(10000);
      //metricConfig.setMinSteps(1);
      metricConfig.setTags("group_" + group.getName());
      logger.info(gson.toJson(metricConfig));
      try {
        session.getRemote().sendString(gson.toJson(metricConfig));
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to send metric polling request", e);
        closeClient();
        connectLatch.countDown();
      }
    }

    @Override
    public void run() {
      while (running.get() && !Thread.currentThread().isInterrupted()) {
        WebSocketClient client = getClient();
        if (!client.isRunning() && !client.isStarting() || client.isFailed()) {
          try {
            connect();
            connectLatch = new CountDownLatch(1);
          } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting up web socket connection", e);
          }
        }
        try {
          connectLatch.await();
        } catch (InterruptedException e) {
        }
      }
    }

    public void stop() {
      running.set(false);
      connectLatch.countDown();
    }

    private void closeClient() {
      try {
        WebSocketClient client = getClient();
        if (client.isRunning()) {
          client.stop();
          client.destroy();
        }
      } catch (Exception e) {
        logger.log(Level.INFO, "error stopping websocket client", e);
      }
    }
  }
}
