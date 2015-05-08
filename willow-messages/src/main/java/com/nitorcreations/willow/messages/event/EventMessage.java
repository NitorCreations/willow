package com.nitorcreations.willow.messages.event;

import com.nitorcreations.willow.messages.AbstractMessage;
import org.msgpack.annotation.Message;

import java.util.HashMap;

@Message
public class EventMessage extends AbstractMessage {

  public String description;
  public String eventType;
  public HashMap<String, String> eventData = new HashMap<>();

}
