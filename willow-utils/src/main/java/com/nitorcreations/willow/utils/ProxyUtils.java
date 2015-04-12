package com.nitorcreations.willow.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.Proxy.Type;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.btr.proxy.selector.pac.PacProxySelector;

public class ProxyUtils {
  private static Logger log = Logger.getLogger(ProxyUtils.class.getCanonicalName());
  private static ConcurrentHashMap<String, PacProxySelector> pacSelectors = new ConcurrentHashMap<>();
  public static Proxy resolveSystemProxy(URI target) {
    if (Boolean.getBoolean("java.net.useSystemProxies")) {
      List<Proxy> l = ProxySelector.getDefault().select(target);
      if (l.size() > 0) {
        return l.get(0);
      }
    }
    String proto = "" + target.getScheme();
    String hostName = target.getHost();
    String proxyUrl = getEnvIgnoreCase(proto.toLowerCase() + "_proxy");
    String proxyAutoconfig = getEnvIgnoreCase("autoconf_proxy");
    String noProxy = getEnvIgnoreCase("no_proxy");
    if (proxyUrl == null && proxyAutoconfig == null) {
      return null;
    } else if (proxyAutoconfig != null) {
      return resolveAutoconfig(proxyAutoconfig, target);
    }
    if (!noProxyMatches(hostName, noProxy)) {
      URL proxyAddr;
      try {
        proxyAddr = new URL(proxyUrl);
      } catch (MalformedURLException e) {
        log.log(Level.INFO, "Failed to resolve proxy for " + target.toString(), e);
        return null;
      }
      return new Proxy(Type.HTTP, new InetSocketAddress(proxyAddr.getHost(), proxyAddr.getPort()));
    } else {
      return null;
    }
  }
  public static synchronized Proxy resolveAutoconfig(String proxyAutoconfig, URI target) {
    PacProxySelector sel = pacSelectors.get(proxyAutoconfig);
    if (sel == null) {
      sel = PacProxySelector.buildPacSelectorForUrl(proxyAutoconfig);
      pacSelectors.put(proxyAutoconfig, sel);
    }
    if (sel == null) return null;
    List<Proxy> l = sel.select(target);
    if (l.size() > 0) {
      return l.get(0);
    }
    return null;
  }
  protected static boolean noProxyMatches(String host, String noProxy) {
    if (noProxy == null)
      return false;
    for (String next : noProxy.split(",")) {
      String trimmed = next.trim();
      while (trimmed.startsWith(".")) {
        trimmed = trimmed.substring(1);
      }
      if (trimmed.equals(host) || host.endsWith("." + trimmed)) {
        return true;
      }
    }
    return false;
  }
  public static String getEnvIgnoreCase(String name) {
    String ret = System.getenv(name.toLowerCase());
    if (ret == null) {
      return System.getenv(name.toUpperCase());
    }
    return ret;
  }
  public static Proxy fromPacResult(String pacResult) {
    return PacProxySelector.buildProxyFromPacResult(pacResult);
  }
  public static InputStream getUriInputStream(String proxyAutoconf, String pacProxyResult, String url) throws IOException, URISyntaxException {
    URI uri = new URI(url);
    Proxy p;
    if (proxyAutoconf != null) {
      p = ProxyUtils.resolveAutoconfig(proxyAutoconf, uri);
    } else if (pacProxyResult != null) {
      p = ProxyUtils.fromPacResult(pacProxyResult);
    } else {
      p = ProxyUtils.resolveSystemProxy(uri);
    }
    URLConnection conn;
    try {
      if (p != null) {
        conn = uri.toURL().openConnection(p);
      } else {
        conn = uri.toURL().openConnection();
      }
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid uri", e);
    }
    if (conn == null) throw new IOException("Failed to get connection to " + url);
    return conn.getInputStream();
  }

}
