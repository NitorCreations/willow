package com.nitorcreations.willow.auth;

import java.util.TreeSet;

import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PublicKeyAuthenticationFilter extends BasicHttpAuthenticationFilter {
  private static final Logger log = LoggerFactory.getLogger(PublicKeyAuthenticationFilter.class);
  private final TreeSet<Long> timeStamps = new TreeSet<>();
  private static final String scheme = "PUBLICKEY";
  public PublicKeyAuthenticationFilter() {
    this.setAuthcScheme(scheme);
    this.setAuthzScheme(scheme);
  }
  @Override
  protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
    String authorizationHeader = getAuthzHeader(request);
    if (authorizationHeader == null || authorizationHeader.length() == 0) {
      return createToken("", null, null, request);
    }

    if (log.isDebugEnabled()) {
        log.debug("Attempting to execute login with headers [" + authorizationHeader + "]");
    }
    
    String[] header = authorizationHeader.split("\\s+");
    if (header.length < 3 || !scheme.equals(header[0].toUpperCase())) {
      return createToken("", null, null, request);
    }
    String[] unameNow = Base64.decodeToString(header[1]).split(":");
    String username = unameNow[0];
    long timestamp = Long.parseLong(unameNow[1]);
    long currentTime = System.currentTimeMillis();
    long wStart = currentTime - 15000;
    long wEnd = currentTime + 15000;
    if (!((timestamp > wStart) && (timestamp < wEnd)) || timeStamps.contains(timestamp)) {
      return createToken("", null, null, request);
    }
    timeStamps.add(timestamp);
    while (timeStamps.first() < wStart) {
      timeStamps.pollFirst();
    }
    PublicKeyAuthenticationToken token = createToken(username, Base64.decode(header[1]), Base64.decode(header[2]), request);
    for (int i=3; i<header.length;i++) {
      token.addSignature(Base64.decode(header[i]));
    }
    return token;
  }
  protected PublicKeyAuthenticationToken createToken(String username, byte[] sign, byte[] signature, ServletRequest request) {
    return new PublicKeyAuthenticationToken(username, sign, signature, getHost(request));
  }
}
