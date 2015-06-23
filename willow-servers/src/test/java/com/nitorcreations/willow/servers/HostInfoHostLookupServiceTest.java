package com.nitorcreations.willow.servers;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.junit.Test;

import com.nitorcreations.willow.messages.HostInfoMessage;
import com.nitorcreations.willow.messages.metrics.MetricConfig;
import com.nitorcreations.willow.metrics.HostInfoMetric;


public class HostInfoHostLookupServiceTest {
  public static class DummyHostInfoMetric extends HostInfoMetric {
    @Override
    public Collection<HostInfoMessage> calculateMetric(MetricConfig conf) {
      HashSet<HostInfoMessage> ret = new LinkedHashSet<>();
      HostInfoMessage testVal = new HostInfoMessage();
      testVal.setInstance("foo");
      testVal.username = "foo";
      testVal.privateHostname = "foo-host";
      ret.add(testVal);
      testVal = new HostInfoMessage();
      testVal.setInstance("test");
      testVal.username = "admin";
      testVal.privateHostname = "test-host";
      ret.add(testVal);
      return ret;
    }      
  }
  public HostInfoHostLookupService test = new HostInfoHostLookupService();
  public HostInfoHostLookupServiceTest() {
    test.hostInfoMetric = new DummyHostInfoMetric();
  }
  @Test
  public void testAdminUser() {
    assertEquals("Test should return 'admin'", "admin", test.getAdminUserFor("test"));
  }
  @Test
  public void testGetResolvableHostname() {
    assertEquals("Test should return 'admin'", "test-host", test.getResolvableHostname("test"));
  }
}
