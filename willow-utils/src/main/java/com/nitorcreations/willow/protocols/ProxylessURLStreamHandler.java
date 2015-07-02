package com.nitorcreations.willow.protocols;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Abstract base class for URL stream handlers where using a proxy is not relevant.
 * The connection is always done without a proxy even if one is requested.
 * 
 * @author mtommila
 */
public abstract class ProxylessURLStreamHandler extends URLStreamHandler {
  @Override
  protected URLConnection openConnection(URL u, Proxy p) throws IOException {
    return openConnection(u);
  }
}
