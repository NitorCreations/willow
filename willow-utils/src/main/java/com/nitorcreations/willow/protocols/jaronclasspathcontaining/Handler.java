package com.nitorcreations.willow.protocols.jaronclasspathcontaining;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {
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
    if (resourceUrl != null && resourceUrl.toString().startsWith("jar:")) {
      String resource = resourceUrl.toString();
      int jarEnd = resource.lastIndexOf("!");
      URL fileUrl = new URL(resource.substring(4, jarEnd));
      return fileUrl.openConnection();
    } else if (resourceUrl != null) {
      throw new IOException("Resource " + u.getPath() + " not found in jar, unable to resolve jar");
    } else {
      throw new IOException("Resource " + u.getPath() + " not found on classpath, unable to resolve jar");
    }
  }
}