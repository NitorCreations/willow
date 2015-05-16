package com.nitorcreations.willow.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.nitorcreations.willow.utils.Obfuscator.KeyDigest;

public class TestObfuscator {
  @Test
  public void testObfuscate() {
    String text = "FooBarBaz";
    Obfuscator obf = new Obfuscator("MyKey");
    assertEquals(text, obf.decrypt(obf.encrypt(text)));
  }

  @Test
  public void testObfuscate2() {
    String text = "FooBarBaz";
    Obfuscator obf = new Obfuscator("MyKey", KeyDigest.SHA_256, 1000);
    assertEquals(text, obf.decrypt(obf.encrypt(text)));
  }
}
