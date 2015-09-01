package com.nitorcreations.willow.messages;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AbstractMessage {
  private String instance = "";
  public long timestamp = System.currentTimeMillis();
  public List<String> tags = new ArrayList<>();
  private String id = null;
  public Map<String, String> extras = new LinkedHashMap<>();
  
  public AbstractMessage() {
    tags.add("category_" + MessageMapping.map(this.getClass()).lcName());
  }

  public void addTags(String... tags) {
    for (String next : tags) {
      this.tags.add(next);
    }
  }

  public void addTags(List<String> tags) {
    this.tags.addAll(tags);
  }
  
  public String getFirstTagWithPrefix(String prefix) {
    for (String next : tags) {
      if (next.startsWith(prefix)) return next;
    }
    return null;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getInstance() {
    return instance;
  }

  public void setInstance(String instance) {
    this.instance = instance;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }
  public void setExtra(String name, String value) {
    extras.put(name, value);
  }
  public Map<String, String> getExtras() {
    return new LinkedHashMap<>(extras);
  }
  public String getExtra(String name) {
    return extras.get(name);
  }
}
