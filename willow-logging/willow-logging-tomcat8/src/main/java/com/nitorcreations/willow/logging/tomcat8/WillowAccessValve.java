package com.nitorcreations.willow.logging.tomcat8;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.ServletException;

import org.apache.catalina.AccessLog;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import com.nitorcreations.logging.WillowAccessLogHelper;

public class WillowAccessValve extends ValveBase implements AccessLog {
  private WillowAccessLogHelper transmitter;

  private boolean _preferProxiedForAddress = true;
  private boolean requestAttrEnabled;
  private String url;
  private long flushInterval = 2000;

  public WillowAccessValve() {
    super(true);
  }

  @Override
  public void invoke(Request request, Response response) throws IOException,
      ServletException {
    getNext().invoke(request, response);
  }

  public void setPreferProxiedForAddress(boolean b) {
    this._preferProxiedForAddress = b;
  }

  public boolean getPreferProxiedForAddress() {
    return _preferProxiedForAddress;
  }

  @Override
  public void log(Request request, Response response, long time) {
    if (transmitter == null) {
      try {
        transmitter = new WillowAccessLogHelper(getFlushInterval(), url);
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException("Invalid logging url " + url);
      }
    }
    transmitter.queue(new AccessLogTomcat8Adapter(request, response, time,
        _preferProxiedForAddress));
  }

  @Override
  public void setRequestAttributesEnabled(boolean requestAttributesEnabled) {
    this.requestAttrEnabled = requestAttributesEnabled;
  }

  @Override
  public boolean getRequestAttributesEnabled() {
    return requestAttrEnabled;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public long getFlushInterval() {
    return flushInterval;
  }

  public void setFlushInterval(long flushInterval) {
    this.flushInterval = flushInterval;
  }

}
