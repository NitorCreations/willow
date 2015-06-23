package com.nitorcreations.willow.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.btr.proxy.selector.pac.PacProxySelector;
import com.btr.proxy.selector.pac.TestUtil;

public class ProxyUtilsTest {
  @Test
  public void testProxyMatch() {
    assertFalse(ProxyUtils.noProxyMatches("foo.bar.com", "localhost,127.0.0.1,localaddress,.localdomain.com"));
    assertTrue(ProxyUtils.noProxyMatches("localhost", "localhost,127.0.0.1,localaddress,.localdomain.com"));
    assertTrue(ProxyUtils.noProxyMatches("localaddress", "localhost,127.0.0.1,localaddress,.localdomain.com"));
    assertTrue(ProxyUtils.noProxyMatches("localaddress.localdomain.com", "localhost,127.0.0.1,localaddress,.localdomain.com"));
    assertTrue(ProxyUtils.noProxyMatches(".localdomain.com", "localhost,127.0.0.1,localaddress,.localdomain.com"));
  }
  @Test
  public void testSystemProxyForClasspath() throws URISyntaxException {
    String old = System.getProperty(ProxyUtils.USE_SYSTEMPROXIES);
    try {
      System.setProperty(ProxyUtils.USE_SYSTEMPROXIES, "true");
      List<Proxy> ret = ProxyUtils.resolveSystemProxy(new URI("classpath:foo"));
      assertTrue("Proxy for classpath should be null", ret == null);
    } finally {
      if (old != null) {
        System.setProperty(ProxyUtils.USE_SYSTEMPROXIES, old);
      } else {
        System.getProperties().remove(ProxyUtils.USE_SYSTEMPROXIES);
      }
    }
  }

  @Test
  public void testProxyAutoconf() throws URISyntaxException {
    Map<String, String> oldEnv = new LinkedHashMap<String, String>(System.getenv());
    Map<String, String> newEnv = new LinkedHashMap<String, String>(oldEnv);
    newEnv.put("autoconf_proxy", "file:src/test/resources/test1.pac");
    try {
      setEnv(newEnv);
      List<Proxy> result = ProxyUtils.resolveSystemProxy(TestUtil.HTTP_TEST_URI);
      assertTrue(PacProxySelector.isEnabled());
      assertTrue("Result should be non null and not empty", result != null && !result.isEmpty());
      assertEquals("Result should have the test proxy", TestUtil.HTTP_TEST_PROXY, result.get(0));
    } finally {
      setEnv(oldEnv);
    }
  }
  protected static void setEnv(Map<String, String> newenv) {
    try {
      Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
      Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
      theEnvironmentField.setAccessible(true);
      Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
      env.putAll(newenv);
      Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
      theCaseInsensitiveEnvironmentField.setAccessible(true);
      Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
      cienv.putAll(newenv);
    }
    catch (NoSuchFieldException e) {
      try {
        Class[] classes = Collections.class.getDeclaredClasses();
        Map<String, String> env = System.getenv();
        for(Class cl : classes) {
          if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Object obj = field.get(env);
            Map<String, String> map = (Map<String, String>) obj;
            map.clear();
            map.putAll(newenv);
          }
        }
      } catch (Exception e2) {
        e2.printStackTrace();
      }
    } catch (Exception e1) {
      e1.printStackTrace();
    } 
  }
}
