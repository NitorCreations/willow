package com.nitorcreations.willow.protocols.classpath;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import com.nitorcreations.willow.protocols.ProxylessURLStreamHandler;

public class Handler extends ProxylessURLStreamHandler {
  private final ClassLoader classLoader;

  public Handler() {
    this.classLoader = getClass().getClassLoader();
  }
  public Handler(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }
  @Override
  protected URLConnection openConnection(URL u) throws IOException {
    final URL resourceUrl = classLoader.getResource(u.getPath());
    if (resourceUrl == null) {
      final URL systemResourceUrl = ClassLoader.getSystemClassLoader().getResource(u.getPath());
      if (systemResourceUrl == null) {
        throw new IOException("Resource " + u.getPath() + " not found on classpath");
      }
      return systemResourceUrl.openConnection();
    }
    return resourceUrl.openConnection();
  }
}