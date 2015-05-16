package com.nitorcreations.willow.logging.logback;

import java.net.URISyntaxException;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import com.nitorcreations.willow.messages.LogMessage;
import com.nitorcreations.willow.messages.LogMessageAdapter;
import com.nitorcreations.willow.messages.WebSocketTransmitter;

public class WebSocketAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
  private String uri;
  private long flushInterval = 2000;
  private WebSocketTransmitter transmitter;

  @Override
  protected void append(final ILoggingEvent event) {

    if (event == null || !isStarted())
      return;
    transmitter.queue(new LogMessage(new LogMessageAdapter() {
      private final ILoggingEvent logEvent = event;
      @Override
      public String getThread() {
        return logEvent.getThreadName();
      }
      
      @Override
      public String getMessage() {
        return logEvent.getFormattedMessage();
      }
      
      @Override
      public String getLogger() {
        return logEvent.getLoggerName();
      }
      
      @Override
      public long getLogEntryTimeStamp() {
        return logEvent.getTimeStamp();
      }
      @Override
      public String getLevel() {
        return logEvent.getLevel().toString();
      }

      @Override
      public String getStackTrace() {
        final IThrowableProxy throwableProxy = logEvent.getThrowableProxy();
        if (throwableProxy != null) {
          return ThrowableProxyUtil.asString(throwableProxy);
        }
        return null;
      }
    }));
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public long getFlushInterval() {
    return flushInterval;
  }

  public void setFlushInterval(long flush_interval) {
    this.flushInterval = flush_interval;
  }

  @Override
  public void start() {
    if (isStarted())
      return;
    if (uri == null) {
      addError("No remote uri configured for appender " + name);
      return;
    }
    try {
      transmitter = WebSocketTransmitter.getSingleton(flushInterval, uri);
      transmitter.start();
      super.start();
    } catch (URISyntaxException e) {
      addError("Invalid uri for appender " + name, e);
    }
  }
}
