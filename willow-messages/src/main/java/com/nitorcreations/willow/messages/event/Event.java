package com.nitorcreations.willow.messages.event;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Event {

  private final static List<String> basicFields = Arrays.asList("description", "tags");

  public String description;
  public List<String> tags = new ArrayList<>();

  public EventMessage getEventMessage() {
    EventMessage msg = new EventMessage();
    msg.description = description;
    msg.tags = tags;
    msg.eventType = this.getClass().getName();

    for (Field f : this.getClass().getFields()) {
      if (basicFields.contains(f.getName())) {
        continue;
      }
      try {
        msg.eventData.put(f.getName(), f.get(this).toString());
      } catch (IllegalAccessException e) {
        Logger.getAnonymousLogger().log(Level.INFO, "Failed to add field to Event", e);
      }
    }

    return msg;
  }

  public void addTag(String tag) {
    tags.add(tag);
  }
}
