package com.nitorcreations.willow.logging.tomcat7;

import java.security.Principal;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

import com.nitorcreations.logging.AccessLogServerAdapter;

public class AccessLogTomcat7Adapter implements AccessLogServerAdapter {

  private final Request request;
  private final Response response;
  private final boolean preferProxiedForAddress;
  private final long time;

  public AccessLogTomcat7Adapter(Request tomcatRequest,
      Response tomcatResponse, long time, boolean preferProxiedForAddress) {
    this.request = tomcatRequest;
    this.response = tomcatResponse;
    this.time = time;
    this.preferProxiedForAddress = preferProxiedForAddress;
  }

  @Override
  public String getAddress() {
    String addr = null;
    if (preferProxiedForAddress) {
      addr = request.getHeader("X-Forwarded-For");
    }
    if (addr == null) {
      addr = request.getRemoteAddr();
    }
    return addr;
  }

  @Override
  public Principal getPrincipal() {
    return request.getPrincipal();
  }

  @Override
  public long getDuration() {
    return time;
  }

  @Override
  public String getMethod() {
    return request.getMethod();
  }

  @Override
  public String getRequestURI() {
    return request.getRequestURI();
  }

  @Override
  public String getProtocol() {
    return request.getProtocol();
  }

  @Override
  public int getStatus() {
    return response.getStatus();
  }

  @Override
  public long getResponseLength() {
    return response.getContentWritten();
  }

  @Override
  public String getReferer() {
    return request.getHeader("Referer");
  }

  @Override
  public String getUserAgent() {
    return request.getHeader("User-Agent");
  }

  @Override
  public long getTimeStamp() {
    return request.getCoyoteRequest().getStartTime();
  }

}
