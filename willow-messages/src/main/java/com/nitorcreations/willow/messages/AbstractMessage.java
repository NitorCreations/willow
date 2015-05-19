package com.nitorcreations.willow.messages;

import java.util.ArrayList;
import java.util.List;

public class AbstractMessage {
  private String instance = "";
  private  long timestamp = System.currentTimeMillis();
  public List<String> tags = new ArrayList<>();
  private String id = null;

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
}
