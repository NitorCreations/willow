package com.nitorcreations.willow.utils;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class TestObfuscatorTool {
  @Test
  public void testTool() throws Exception {
    ObfuscatorTool.main(new String[] {"target/test-classes/root.properties"});
    File enc = new File("target/test-classes/root.properties.encrypted");
    File key = new File("target/test-classes/root.properties.key");
    assertTrue(enc.exists());
    assertTrue(key.exists());
    System.setProperty("decrypt", "");
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(output, true, "UTF-8"));
    ObfuscatorTool.main(new String[] {"target/test-classes/root.properties.encrypted", "target/test-classes/root.properties.key" });
    System.setOut(oldOut);
    byte[] outputData = output.toByteArray();
    ByteArrayInputStream in = new ByteArrayInputStream(outputData);
    MergeableProperties props = new MergeableProperties();
    props.load(in);
    assertEquals("foo/bar", props.getProperty("included.file[1].extraprops"));
  }
}
