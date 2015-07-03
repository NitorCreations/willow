package com.nitorcreations.willow.eventhandler;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
import com.google.gson.JsonObject;
import com.nitorcreations.willow.messages.event.EventMessage;
import com.nitorcreations.willow.messages.metrics.MetricConfig;
import com.nitorcreations.willow.sshagentauth.SSHUtil;

/**
 * Event poller for receiving events from metrics server via websocket and dispatching them to event handlers.
 * 
 * @author mtommila
 */
public class EventPoller {
  private Logger logger = Logger.getLogger(getClass().getName());

  private Map<String, List<EventHandler>> eventHandlers;

  @Inject
  private Gson gson;

  @Inject
  private ExecutorService executorService;

  /**
   * Initialize the event poller.
   * 
   * @param eventHandlers The handlers for different event types.
   * @param uri The web socket connection URI.
   */
  public void initialize(Map<String, List<EventHandler>> eventHandlers, URI uri) {
    this.eventHandlers = eventHandlers;
    EventListener listener = new EventListener(uri);
    executorService.submit(listener);
    logger.fine("Started event listener");
  }

  /**
   * Websocket message listener. Dispatches events to the event handlers.
   * Autowired to Jetty websocket implementation via annotations.
   * 
   * @author mtommila
   */
  @WebSocket
  public class EventListener implements Runnable {

    private Logger logger = Logger.getLogger(getClass().getName());

    private URI uri;
    private WebSocketClient client;
    private CountDownLatch connectLatch;
    private Set<String> messageIds = new HashSet<>();

    private EventListener(URI uri) {
      this.uri = uri;
    }

    private synchronized WebSocketClient getClient() {
      if (this.client == null) {
        this.client = new WebSocketClient();
      }
      return this.client;
    }

    private synchronized WebSocketClient createClient() {
      this.client = new WebSocketClient();
      return this.client;
    }

    private void connect() throws Exception {
      logger.info("Connecting event listener");
      WebSocketClient client = createClient();
      client.start();
      client.setAsyncWriteTimeout(5000);
      client.setConnectTimeout(2000);
      client.setStopTimeout(5000);
      ClientUpgradeRequest request = new ClientUpgradeRequest();
      request.setHeader("Authorization", SSHUtil.getPublicKeyAuthorization(System.getProperty("user.name", "willow")));
      Future<Session> future = client.connect(this, uri, request);
      logger.info("Connecting to : " + uri);
      try {
        future.get();
        logger.info("Connected to : " + uri);
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Failed to connect event poller with uri " + uri, e);
        if (client.isRunning()) {
          client.stop();
          client.destroy();
        }
        throw e;
      }
    }

    @OnWebSocketMessage
    public void messageReceived(Session session, String message) {
      if (message == null || message.isEmpty()) {
        return;
      }
      EventMessage[] eventMessages = null;
      List<EventMessage> uniqueEventMessages = new ArrayList<>();
      try {
        eventMessages = gson.fromJson(gson.fromJson(message, JsonObject.class).get("data"), EventMessage[].class);
      } catch (Exception e) {
        logger.log(Level.INFO, "Failure in unmarshalling event data", e);
      }
      if (eventMessages != null) {
        // Loop through all events that happened during the retrieved time window
        synchronized (messageIds) {
          Set<String> newMessageIds = new HashSet<>();
          for (EventMessage eventMessage : eventMessages) {
            if (eventMessage != null && eventMessage.eventType != null && eventMessage.getId() != null) {
              String id = eventMessage.getId();
              newMessageIds.add(id);
              // Ignore duplicate messages
              if (!messageIds.contains(id)) {
                uniqueEventMessages.add(eventMessage);
              }
            }
          }
          // Clear the previous message IDs and set the now received message IDs, note the synchronization
          messageIds.clear();
          messageIds.addAll(newMessageIds);
        }
        // Loop through configured handlers to handle the non-duplicate message
        for (EventMessage eventMessage : uniqueEventMessages) {
          List<EventHandler> messageEventHandlers = eventHandlers.get(eventMessage.eventType);
          if (messageEventHandlers != null) {
            for (EventHandler eventHandler : messageEventHandlers) {
              try {
                eventHandler.handle(eventMessage);
              } catch (Exception e) {
                logger.log(Level.INFO, "Failure in handling event data", e);
              }
            }
          }
        }
      }
    }

    @OnWebSocketClose
    public void handleClose(Session closedSession, int statusCode, String reason) {
      logger.log(Level.INFO, "Event listener websocket closed with status " + statusCode + ". Reason: " + reason);
      closeClient();
      connectLatch.countDown();
    }

    @OnWebSocketError
    public void handleError(Session errorSession, Throwable throwable) {
      logger.log(Level.INFO, "Event listener websocket error", throwable);
      closeClient();
      connectLatch.countDown();
    }

    @OnWebSocketConnect
    public void startPolling(Session session) {
      logger.info("Sending poll request for events");
      MetricConfig metricConfig = new MetricConfig();
      long now = System.currentTimeMillis();
      metricConfig.setId("eventhandler");
      metricConfig.setMetricKey("/event");
      metricConfig.setStart(now - 20000);   // For the first chunk, send events from 20s ago
      metricConfig.setStop(now);            // ... up to the current moment.
      metricConfig.setStep(10000);          // Then keep sending data every 10 seconds.
      metricConfig.setMinSteps(2);          // Every time send data for the last 2 * 10 = 20 seconds.
      String json = gson.toJson(metricConfig);
      logger.info("Sending polling request " + json);
      try {
        session.getRemote().sendString(json);
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to send event polling request", e);
        closeClient();
        connectLatch.countDown();
      }
    }

    @Override
    public void run() {
      while (!Thread.currentThread().isInterrupted()) {
        WebSocketClient client = getClient();
        if (!client.isRunning() && !client.isStarting() || client.isFailed()) {
          try {
            connect();
            connectLatch = new CountDownLatch(1);
          } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting up websocket connection", e);
          }
        }
        try {
          connectLatch.await();
        } catch (InterruptedException e) {
        }
      }
    }

    private void closeClient() {
      try {
        WebSocketClient client = getClient();
        if (client.isRunning()) {
          client.stop();
          client.destroy();
        }
      } catch (Exception e) {
        logger.log(Level.INFO, "Error stopping websocket client", e);
      }
    }
  }
}
