package com.nitorcreations.willow.simulate;

import com.nitorcreations.willow.messages.AbstractMessage;

public interface MessageProducer {
  AbstractMessage next();
}
