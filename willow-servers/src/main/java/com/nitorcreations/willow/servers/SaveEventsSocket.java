package com.nitorcreations.willow.servers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;

import com.google.gson.Gson;
import com.nitorcreations.willow.messages.AbstractMessage;
import com.nitorcreations.willow.messages.HashMessage;
import com.nitorcreations.willow.messages.LongStatisticsMessage;
import com.nitorcreations.willow.messages.MessageMapping;
import com.nitorcreations.willow.messages.MessageMapping.MessageType;

@WebSocket
public class SaveEventsSocket {
  private final CountDownLatch closeLatch;
  private final MessageMapping mapping = new MessageMapping();
  private final Client client = MetricsServlet.getClient();
  @SuppressWarnings("unused")
  private Session session;
  private String path;
  private List<String> tags;

  public SaveEventsSocket() {
    this.closeLatch = new CountDownLatch(1);
  }

  public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
    return this.closeLatch.await(duration, unit);
  }

  @OnWebSocketClose
  public void onClose(int statusCode, String reason) {
    System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
    this.session = null;
    this.closeLatch.countDown();
  }

  @OnWebSocketConnect
  public void onConnect(Session session) {
    System.out.printf("Got connect: %s%n", session);
    this.session = session;
    path = session.getUpgradeRequest().getRequestURI().getPath().substring("/statistics/".length());
    tags = session.getUpgradeRequest().getParameterMap().get("tag");
    if (tags == null) {
      tags = new ArrayList<>();
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @OnWebSocketMessage
  public void messageReceived(byte buf[], int offset, int length) {
    try {
      Gson gson = new Gson();
      for (AbstractMessage msgObject : mapping.decode(buf, offset, length)) {
        MessageType type = MessageMapping.map(msgObject.getClass());
        Object stored = msgObject;
        if (type == MessageType.LONGSTATS) {
          stored = ((LongStatisticsMessage) msgObject).getMap();
          String instance = (String) ((Map) stored).get("instance");
          if (instance == null || instance.isEmpty()) {
            ((Map) stored).put("instance", path);
          }
          List<String> tmpTags = new ArrayList<>();
          tmpTags.addAll(tags);
          tmpTags.addAll(msgObject.tags);
          ((Map) stored).put("tags", tmpTags);
          ((Map) stored).put("timestamp", msgObject.timestamp);
        } else if (type == MessageType.HASH) {
          stored = ((HashMessage) msgObject).getMap();
          String instance = (String) ((Map) stored).get("instance");
          if (instance == null || instance.isEmpty()) {
            ((Map) stored).put("instance", path);
          }
          List<String> tmpTags = new ArrayList<>();
          tmpTags.addAll(tags);
          tmpTags.addAll(msgObject.tags);
          ((Map) stored).put("tags", tmpTags);
          ((Map) stored).put("timestamp", msgObject.timestamp);
        } else {
          if (msgObject.instance == null || msgObject.instance.isEmpty()) {
            msgObject.instance = path;
          }
          msgObject.addTags(tags);
        }
        String source = gson.toJson(stored);
        if (System.getProperty("debug") != null) {
          System.out.println(type.lcName() + ": " + source);
        }
        IndexResponse resp = client.prepareIndex(getIndex(msgObject.timestamp), type.lcName()).setSource(source).execute().actionGet(1000);
        if (!resp.isCreated()) {
          System.out.println("Failed to create index for " + source);
        }
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private static String getIndex(long timestamp) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date(timestamp));
    return String.format("%04d", cal.get(Calendar.YEAR)) + "-" + String.format("%02d", cal.get(Calendar.MONTH) + 1) + "-" + String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
  }
}
