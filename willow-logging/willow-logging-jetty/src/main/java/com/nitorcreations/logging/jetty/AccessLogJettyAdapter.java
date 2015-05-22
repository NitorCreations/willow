package com.nitorcreations.logging.jetty;

import java.security.Principal;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;

import com.nitorcreations.logging.AccessLogServerAdapter;

public class AccessLogJettyAdapter implements AccessLogServerAdapter {
  private final Request request;
  private final int status;
  private final long written;
  private boolean preferProxiedForAddress;

  public AccessLogJettyAdapter(Request request, int status, long written,
      boolean preferProxiedForAddress) {
    this.request = request;
    this.status = status;
    this.written = written;
    this.preferProxiedForAddress = preferProxiedForAddress;
  }

  @Override
  public String getAddress() {
    String addr = null;
    if (preferProxiedForAddress) {
      addr = request.getHeader(HttpHeader.X_FORWARDED_FOR.toString());
    }
    if (addr == null) {
      addr = request.getRemoteAddr();
    }
    return addr;
  }

  @Override
  public Principal getPrincipal() {
    Authentication authentication = request.getAuthentication();
    if (authentication instanceof Authentication.User) {
      return ((Authentication.User) authentication).getUserIdentity()
          .getUserPrincipal();
    }
    return null;
  }

  @Override
  public long getTimeStamp() {
    return request.getTimeStamp();
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
    return status;
  }

  @Override
  public long getResponseLength() {
    return written;
  }

  @Override
  public String getReferer() {
    return request.getHeader(HttpHeader.REFERER.toString());
  }

  @Override
  public String getUserAgent() {
    return request.getHeader(HttpHeader.USER_AGENT.toString());
  }

  @Override
  public long getDuration() {
    long now = System.currentTimeMillis();
    return now - getTimeStamp();
  }

}
