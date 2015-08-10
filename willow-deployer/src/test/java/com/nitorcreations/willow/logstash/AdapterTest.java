package com.nitorcreations.willow.logstash;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;

import com.nitorcreations.willow.messages.LogMessage;

public class AdapterTest {
  @Test
  public void testAdapter() {
    Adapter adptr = new Adapter();
    assertEquals(LogMessage.class, adptr.fromHash("log", new HashMap<>()).getClass());
  }
  

}
