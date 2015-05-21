package com.nitorcreations.willow.utils;

import java.io.IOException;

public class ObfustcatorPropertySource implements PropertySource {
  private final Obfuscator obfuscator;
  public ObfustcatorPropertySource() throws IOException {
    this(new Obfuscator());
  }
  public ObfustcatorPropertySource(Obfuscator obfuscator) {
    this.obfuscator = obfuscator;
  }
  @Override
  public String getProperty(String key) {
    String ret = obfuscator.decrypt(key);
    if (ret == null) return key;
    return ret;
  }
}