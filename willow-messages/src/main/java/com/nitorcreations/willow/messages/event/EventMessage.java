package com.nitorcreations.willow.messages.event;

import java.util.HashMap;

import org.msgpack.annotation.Message;

import com.nitorcreations.willow.messages.AbstractMessage;

@Message
public class EventMessage extends AbstractMessage {

  public String description;
  public String eventType;
  public HashMap<String, String> eventData = new HashMap<>();

}
