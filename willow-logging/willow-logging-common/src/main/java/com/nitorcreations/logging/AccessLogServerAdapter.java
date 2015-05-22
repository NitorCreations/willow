package com.nitorcreations.logging;

import java.security.Principal;

public interface AccessLogServerAdapter {
  String getAddress();

  Principal getPrincipal();

  String getMethod();

  String getRequestURI();

  String getProtocol();

  int getStatus();

  long getResponseLength();

  String getReferer();

  String getUserAgent();

  long getTimeStamp();

  long getDuration();
}
