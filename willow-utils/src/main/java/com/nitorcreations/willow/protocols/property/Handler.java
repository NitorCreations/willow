package com.nitorcreations.willow.protocols.property;

import java.net.URL;
import java.net.URLConnection;

import com.nitorcreations.willow.protocols.ProxylessURLStreamHandler;

public class Handler extends ProxylessURLStreamHandler {
  public Handler() {
  }
  @Override
  protected URLConnection openConnection(URL u) {
    return new PropertyUrlConnection(u);
  }
}