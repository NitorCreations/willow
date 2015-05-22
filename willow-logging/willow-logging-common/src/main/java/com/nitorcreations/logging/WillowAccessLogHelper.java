package com.nitorcreations.logging;

import java.net.URISyntaxException;
import java.security.Principal;

import com.nitorcreations.willow.messages.AccessLogEntry;
import com.nitorcreations.willow.messages.WebSocketTransmitter;

public class WillowAccessLogHelper {
  private WebSocketTransmitter transmitter;
  private final long flushInterval;
  private final String url;

  public WillowAccessLogHelper(long flushInterval, String url)
      throws URISyntaxException {
    super();
    this.flushInterval = flushInterval;
    this.url = url;
  }

  public void queue(AccessLogServerAdapter req) {
    if (transmitter == null) {
      try {
        transmitter = WebSocketTransmitter.getSingleton(flushInterval, url);
        transmitter.start();
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException("Invalid logging url", e);
      }
    }
    AccessLogEntry msg = new AccessLogEntry();
    msg.setRemoteAddr(req.getAddress());
    Principal p = req.getPrincipal();
    if (p != null) {
      msg.setAuthentication(p.getName());
    }
    msg.setTimestamp(req.getTimeStamp());
    msg.setMethod(req.getMethod());
    msg.setUri(req.getRequestURI());
    msg.setProtocol(req.getProtocol());
    int status = req.getStatus();
    if (status <= 0) {
      status = 404;
    }
    msg.setStatus(status);
    msg.setResponseLength(req.getResponseLength());
    msg.setDuration(req.getDuration());
    msg.setReferrer(req.getReferer());
    msg.setAgent(req.getUserAgent());
    transmitter.queue(msg);
  }
}
