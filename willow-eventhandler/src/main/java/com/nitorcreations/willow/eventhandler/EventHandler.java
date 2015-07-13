package com.nitorcreations.willow.eventhandler;

import com.nitorcreations.willow.messages.event.EventMessage;

/**
 * Generic event handler interface.
 * 
 * @author mtommila
 */
public interface EventHandler {
  /**
   * Handle the event.
   * 
   * @param eventMessage The event message.
   */
  void handle(EventMessage eventMessage) throws Exception;
}
