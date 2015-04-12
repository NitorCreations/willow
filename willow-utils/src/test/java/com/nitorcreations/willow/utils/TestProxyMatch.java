package com.nitorcreations.willow.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestProxyMatch {
  @Test
  public void testProxyMatch() {
    assertFalse(ProxyUtils.noProxyMatches("foo.bar.com", "localhost,127.0.0.1,localaddress,.localdomain.com"));
    assertTrue(ProxyUtils.noProxyMatches("localhost", "localhost,127.0.0.1,localaddress,.localdomain.com"));
    assertTrue(ProxyUtils.noProxyMatches("localaddress", "localhost,127.0.0.1,localaddress,.localdomain.com"));
    assertTrue(ProxyUtils.noProxyMatches("localaddress.localdomain.com", "localhost,127.0.0.1,localaddress,.localdomain.com"));
    assertTrue(ProxyUtils.noProxyMatches(".localdomain.com", "localhost,127.0.0.1,localaddress,.localdomain.com"));
  }
}
