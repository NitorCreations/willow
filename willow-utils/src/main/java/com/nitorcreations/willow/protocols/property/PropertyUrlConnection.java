package com.nitorcreations.willow.protocols.property;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Properties;


public class PropertyUrlConnection extends URLConnection {
  public static final ThreadLocal<Properties> currentProperties = new ThreadLocal<>();

  protected PropertyUrlConnection(URL url) {
    super(url);
  }
  @Override
  public void connect() throws IOException {
  }
  @Override
  public InputStream getInputStream() throws IOException {
    String property = null;
    if (currentProperties.get() != null) {
      property = currentProperties.get().getProperty(url.getPath());
    }
    if (property == null) throw new IOException("Property '" + url.getPath() + "' not found");
    return new ByteArrayInputStream(property.getBytes(StandardCharsets.UTF_8));
  }
}
