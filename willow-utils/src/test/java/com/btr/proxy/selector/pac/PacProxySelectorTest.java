package com.btr.proxy.selector.pac;

import static com.btr.proxy.selector.pac.TestUtil.toUrl;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

import org.junit.Test;

/*****************************************************************************
 * Tests for the Pac script parser and proxy selector.
 * 
 * @author Bernd Rosstauscher (proxyvole@rosstauscher.de) Copyright 2009
 ****************************************************************************/

public class PacProxySelectorTest {

  /*************************************************************************
   * Test method
   * 
   * @throws ProxyException
   *           on proxy detection error.
   * @throws MalformedURLException
   *           on URL erros
   ************************************************************************/
  @Test
  public void testScriptExecution() throws Exception {
    PacProxySelector test = PacProxySelector
        .buildPacSelectorForUrl(toUrl("test1.pac"));
    List<Proxy> result = test.select(TestUtil.HTTP_TEST_URI);
    assertTrue(test.isEnabled());
    assertEquals(TestUtil.HTTP_TEST_PROXY, result.get(0));
  }
  @Test
  public void testNullUrl() throws Exception {
  }
  /*************************************************************************
   * Test method
   * 
   * @throws ProxyException
   *           on proxy detection error.
   * @throws MalformedURLException
   *           on URL erros
   ************************************************************************/
  @Test
  public void testScriptExecution2() throws Exception {
    PacProxySelector pacProxySelector = new PacProxySelector(
        new UrlPacScriptSource(toUrl("test2.pac")));
    List<Proxy> result = pacProxySelector.select(TestUtil.HTTP_TEST_URI);
    assertEquals(Proxy.NO_PROXY, result.get(0));

    result = pacProxySelector.select(TestUtil.HTTPS_TEST_URI);
    assertEquals(Proxy.NO_PROXY, result.get(0));
  }

  /*************************************************************************
   * Test download fix to prevent infinite loop.
   * 
   * @throws ProxyException
   *           on proxy detection error.
   * @throws MalformedURLException
   *           on URL erros
   ************************************************************************/
  @Test
  public void pacDownloadFromURLShouldNotUseProxy() throws Exception {
    ProxySelector oldOne = ProxySelector.getDefault();
    try {
      ProxySelector.setDefault(new ProxySelector() {
        @Override
        public List<Proxy> select(URI uri) {
          throw new IllegalStateException("Should not download via proxy");
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
          // Not used
        }
      });

      PacProxySelector pacProxySelector = new PacProxySelector(
          new UrlPacScriptSource("http://www.test.invalid/wpad.pac"));
      pacProxySelector.select(TestUtil.HTTPS_TEST_URI);
    } finally {
      ProxySelector.setDefault(oldOne);
    }
  }

  /*************************************************************************
   * Test method
   * 
   * @throws ProxyException
   *           on proxy detection error.
   * @throws MalformedURLException
   *           on URL erros
   ************************************************************************/
  @Test
  public void testScriptMuliProxy() throws Exception {
    PacProxySelector pacProxySelector = new PacProxySelector(
        new UrlPacScriptSource(toUrl("testMultiProxy.pac")));
    List<Proxy> result = pacProxySelector.select(TestUtil.HTTP_TEST_URI);
    assertEquals(2, result.size());
    assertEquals(
        new Proxy(Type.HTTP, InetSocketAddress.createUnresolved("my-proxy.com",
            80)), result.get(0));
    assertEquals(
        new Proxy(Type.HTTP, InetSocketAddress.createUnresolved(
            "my-proxy2.com", 8080)), result.get(1));
  }

  /*************************************************************************
   * Test method for the override local IP feature.
   * 
   * @throws ProxyException
   *           on proxy detection error.
   * @throws MalformedURLException
   *           on URL erros
   ************************************************************************/
  @Test
  public void testLocalIPOverride() throws Exception {
    System.setProperty(PacScriptMethods.OVERRIDE_LOCAL_IP, "123.123.123.123");
    try {
      PacProxySelector pacProxySelector = new PacProxySelector(
          new UrlPacScriptSource(toUrl("testLocalIP.pac")));
      List<Proxy> result = pacProxySelector.select(TestUtil.HTTP_TEST_URI);
      assertEquals(
          result.get(0),
          new Proxy(Type.HTTP, InetSocketAddress.createUnresolved(
              "123.123.123.123", 8080)));
    } finally {
      System.setProperty(PacScriptMethods.OVERRIDE_LOCAL_IP, "");
    }

  }

}
