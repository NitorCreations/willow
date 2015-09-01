package com.nitorcreations.willow.logstash;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.junit.Test;

import com.nitorcreations.willow.messages.LogMessage;
import com.nitorcreations.willow.utils.MergeableProperties;

public class AdapterTest {
  @Test
  public void testAdapter() {
    Adapter adptr = new Adapter();
    assertEquals(LogMessage.class, adptr.fromHash("log", new HashMap<String, Object>()).getClass());
  }
  @Test
  public void testProps() throws MalformedURLException {
    MergeableProperties p = new MergeableProperties();
    URL url = new URL("classpath:logstash.properties?deployer.launch.env.keys.appendchar=,&deployer.launch.env.keys=JAVA_OPTIONS&JAVA_OPTIONS=-javaagent:../../target/jacoco-agent.jar=jmx=true,destfile=../../target/willow-deployer/server.exec&deployer.launch.workdir=target/logstash&deployer.launch.workdir.readonly=true");
    assertEquals("deployer.launch.env.keys.appendchar=,&deployer.launch.env.keys=JAVA_OPTIONS&JAVA_OPTIONS=-javaagent:../../target/jacoco-agent.jar=jmx=true,destfile=../../target/willow-deployer/server.exec&deployer.launch.workdir=target/logstash&deployer.launch.workdir.readonly=true", url.getQuery());
    p.merge(System.getProperties(), url.toString());
    assertEquals("JAVA_OPTIONS,VENDORED_JRUBY", p.get("deployer.launch.env.keys"));
  }
}