package com.nitorcreations.willow.servers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.inject.Inject;
import javax.inject.Named;

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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@WebSocket
@Named
public class SaveEventsSocket extends BasicWillowSocket {
  private final MessageMapping mapping = new MessageMapping();
  @Inject
  private Client client;
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
  @SuppressWarnings({ "rawtypes" })
  @SuppressFBWarnings(value={"BC_UNCONFIRMED_CAST_OF_RETURN_VALUE"}, justification="Messagetype encoded in the message used to determine type")
  @OnWebSocketMessage
  public void messageReceived(byte buf[], int offset, int length) {
    try {
      Gson gson = new Gson();
      for (AbstractMessage msgObject : mapping.decode(buf, offset, length)) {
        MessageType type = MessageMapping.map(msgObject.getClass());
        Object stored = msgObject;
        if (type == MessageType.LONGSTATS) {
          stored = ((LongStatisticsMessage) msgObject).getMap();
          addFieldsToStoredMap(msgObject, (Map)stored);
        } else if (type == MessageType.HASH) {
          stored = ((HashMessage) msgObject).getMap();
          addFieldsToStoredMap(msgObject, (Map)stored);
        } else {
          if (msgObject.getInstance() == null || msgObject.getInstance().isEmpty()) {
            msgObject.setInstance(path);
          }
          msgObject.addTags(tags);
        }
        String source = gson.toJson(stored);
        if (System.getProperty("debug") != null) {
          System.out.println(type.lcName() + ": " + source);
        }
        IndexResponse resp = client.prepareIndex(getIndex(msgObject.getTimestamp()), type.lcName()).setSource(source).execute().actionGet(5000);
        if (!resp.isCreated()) {
          log.warning("Failed to create index for " + source);
        }
      }
    } catch (Exception e) {
      log.log(Level.INFO, "Exception while receiving message", e);
    }
  }
  @SuppressWarnings("unchecked")
  private void addFieldsToStoredMap(AbstractMessage msgObject, Map stored) {
    String instance = (String) stored.get("instance");
    if (instance == null || instance.isEmpty()) {
      stored.put("instance", path);
    }
    List<String> tmpTags = new ArrayList<>();
    tmpTags.addAll(tags);
    tmpTags.addAll(msgObject.tags);
    stored.put("tags", tmpTags);
    stored.put("timestamp", msgObject.getTimestamp());
    
  }
  private static String getIndex(long timestamp) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date(timestamp));
    return String.format("%04d", cal.get(Calendar.YEAR)) + "-" + String.format("%02d", cal.get(Calendar.MONTH) + 1) + "-" + String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
  }
}
