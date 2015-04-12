package com.nitorcreations.willow.protocols.sftp;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {
  @Override
  protected URLConnection openConnection(URL u) throws IOException {
    return new SftpURLConnection(u);
  }
  @Override
  protected URLConnection openConnection(URL u, Proxy p) throws IOException {
    return new SftpURLConnection(u, p);
  };
  @Override
  public int getDefaultPort() {
    return 22;
  }
}
