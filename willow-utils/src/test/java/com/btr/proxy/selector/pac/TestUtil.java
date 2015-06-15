package com.btr.proxy.selector.pac;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.Proxy.Type;
import java.nio.file.Path;
import java.nio.file.Paths;

/*****************************************************************************
 * This class defines some constants and helper methods for the unit tests.
 * 
 * @author Bernd Rosstauscher (proxyvole@rosstauscher.de) Copyright 2009
 ****************************************************************************/

public class TestUtil {

  public static final Path TEST_DATA_FOLDER = Paths.get("src", "test",
      "resources");

  public static final Proxy HTTP_TEST_PROXY = new Proxy(Type.HTTP,
      InetSocketAddress.createUnresolved("http_proxy.unit-test.invalid", 8090));
  public static final Proxy HTTPS_TEST_PROXY = new Proxy(Type.HTTP,
      InetSocketAddress.createUnresolved("https_proxy.unit-test.invalid", 8091));
  public static final Proxy FTP_TEST_PROXY = new Proxy(Type.HTTP,
      InetSocketAddress.createUnresolved("ftp_proxy.unit-test.invalid", 8092));
  public static final Proxy SOCKS_TEST_PROXY = new Proxy(Type.SOCKS,
      InetSocketAddress.createUnresolved("socks_proxy.unit-test.invalid", 8095));

  public static final URI NO_PROXY_TEST_URI;
  public static final URI HTTP_TEST_URI;
  public static final URI HTTPS_TEST_URI;
  public static final URI FTP_TEST_URI;
  public static final URI SOCKS_TEST_URI;
  public static final URI LOCAL_TEST_URI;
  public static final URI SOCKET_TEST_URI;

  // Setup some testing constants.
  static {
    try {
      NO_PROXY_TEST_URI = new URI("http://no_proxy.unit-test.invalid/");
      HTTP_TEST_URI = new URI("http://host1.unit-test.invalid/");
      HTTPS_TEST_URI = new URI("https://host1.unit-test.invalid/");
      FTP_TEST_URI = new URI("ftp://host1.unit-test.invalid/");
      SOCKS_TEST_URI = new URI("socks://host1.unit-test.invalid/");
      LOCAL_TEST_URI = new URI("http://myhost");
      SOCKET_TEST_URI = new URI("socket://host1.unit-test.invalid/");
    } catch (URISyntaxException e) {
      throw new RuntimeException("URI error" + e.getMessage());
    }
  }

  public static String toUrl(String testFile) throws MalformedURLException {
    return new File(TEST_DATA_FOLDER.toFile(), testFile).toURI().toURL()
        .toString();
  }
}
