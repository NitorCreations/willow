package com.nitorcreations.willow.messages.event;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public abstract class Event {

  private final static List<String> basicFields = Arrays.asList("description");

  public String description;

  public EventMessage getEventMessage() {
    EventMessage msg = new EventMessage();
    msg.description = description;
    msg.eventType = this.getClass().getName();

    for (Field f : this.getClass().getFields()) {
      if (basicFields.contains(f.getName())) {
        continue;
      }
      try {
        msg.eventData.put(f.getName(), f.get(this).toString());
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }

    return msg;
  }

}
