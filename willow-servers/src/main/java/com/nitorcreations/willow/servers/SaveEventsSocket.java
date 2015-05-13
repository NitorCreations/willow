package com.nitorcreations.willow.servers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;

import com.google.gson.Gson;
import com.nitorcreations.willow.messages.AbstractMessage;
import com.nitorcreations.willow.messages.HashMessage;
import com.nitorcreations.willow.messages.LongStatisticsMessage;
import com.nitorcreations.willow.messages.MessageMapping;
import com.nitorcreations.willow.messages.MessageMapping.MessageType;

@WebSocket
@Named
public class SaveEventsSocket extends BasicWillowSocket {
  private final MessageMapping mapping = new MessageMapping();
  @Inject
  private Node node;
  private String path;
  private List<String> tags;

  @OnWebSocketConnect
  public void onConnect(Session session) {
    super.onConnect(session);
    path = session.getUpgradeRequest().getRequestURI().getPath().substring("/statistics/".length());
    tags = session.getUpgradeRequest().getParameterMap().get("tag");
    if (tags == null) {
      tags = new ArrayList<>();
    }
  }
  @OnWebSocketClose
  public void onClose(int statusCode, String reason) {
    super.onClose(statusCode, reason);
  }
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @OnWebSocketMessage
  public void messageReceived(byte buf[], int offset, int length) {
    try (Client client = node.client()){
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
        IndexResponse resp = client.prepareIndex(getIndex(msgObject.timestamp), type.lcName()).setSource(source).execute().actionGet(5000);
        if (!resp.isCreated()) {
          log.warning("Failed to create index for " + source);
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
