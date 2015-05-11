package com.nitorcreations.willow.protocols.property;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {
  public Handler() {
  }
  @Override
  protected URLConnection openConnection(URL u) {
    return new PropertyUrlConnection(u);
  }
}