package com.nitorcreations.willow.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.btr.proxy.selector.pac.PacProxySelector;

public class ProxyUtils {
  private static Logger log = Logger.getLogger(ProxyUtils.class.getCanonicalName());
  private static ConcurrentHashMap<String, PacProxySelector> pacSelectors = new ConcurrentHashMap<>();
  public static final String USE_SYSTEMPROXIES = "java.net.useSystemProxies";
  public static List<Proxy> resolveSystemProxy(URI target) {
    if (Boolean.getBoolean(USE_SYSTEMPROXIES)) {
      List<Proxy> l = null;
      try {
        l = ProxySelector.getDefault().select(target);
      } catch (IllegalArgumentException e) {
        log.log(Level.FINE, "Failed to resolve with default system proxy", e);
      }
      if (l !=  null && !l.isEmpty()) {
        return l;
      }
    }
    String proto = "" + target.getScheme();
    String hostName = target.getHost();
    String proxyUrl = getEnvIgnoreCase(proto.toLowerCase(Locale.ENGLISH) + "_proxy");
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
      return Collections.singletonList(new Proxy(Type.HTTP, new InetSocketAddress(proxyAddr.getHost(), proxyAddr.getPort())));
    } else {
      return null;
    }
  }
  public static synchronized List<Proxy> resolveAutoconfig(String proxyAutoconfig, URI target) {
    PacProxySelector sel = pacSelectors.get(proxyAutoconfig);
    if (sel == null) {
      sel = PacProxySelector.buildPacSelectorForUrl(proxyAutoconfig);
      if (sel != null) {
        pacSelectors.putIfAbsent(proxyAutoconfig, sel);
      } else {
        return null;
      }
    }
    sel = pacSelectors.get(proxyAutoconfig);
    List<Proxy> l = sel.select(target);
    if (l.size() > 0) {
      return l;
    }
    return null;
  }
  protected static boolean noProxyMatches(String host, String noProxy) {
    if (noProxy == null) {
      return false;
    }
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
    String ret = System.getenv(name.toLowerCase(Locale.ENGLISH));
    if (ret == null) {
      return System.getenv(name.toUpperCase(Locale.ENGLISH));
    }
    return ret;
  }
  public static List<Proxy> fromPacResult(String pacResult) {
    List<Proxy> ret = new ArrayList<>();
    for (String next : pacResult.split(";")) {
      ret.add(PacProxySelector.buildProxyFromPacResult(next));
    }
    return ret;
  }
  public static InputStream getUriInputStream(String proxyAutoconf, String pacProxyResult, String url) throws IOException, URISyntaxException {
    return getUriInputStream(proxyAutoconf, pacProxyResult, url, null);
  }
  public static InputStream getUriInputStream(String proxyAutoconf, String pacProxyResult, String url, RequestCustomizer cust) throws IOException, URISyntaxException {
    URI uri = new URI(url);
    List<Proxy> l;
    if (proxyAutoconf != null) {
      l = ProxyUtils.resolveAutoconfig(proxyAutoconf, uri);
    } else if (pacProxyResult != null) {
      l = ProxyUtils.fromPacResult(pacProxyResult);
    } else {
      l = ProxyUtils.resolveSystemProxy(uri);
    }
    URLConnection conn = null;
    try {
      IOException lastEx = new IOException("Failed to get working proxy");
      if (l != null && l.size() > 0) {
        for (Proxy p : l) {
          try {
            conn = uri.toURL().openConnection(p);
            break;
          } catch (IOException e) {
            lastEx.addSuppressed(e);
          }
        }
        if (conn == null) {
          throw lastEx;
        }
      } else {
        conn = uri.toURL().openConnection();
      }
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid uri", e);
    }
    if (conn == null) {
      throw new IOException("Failed to get connection to " + url);
    }
    if (cust != null) {
      cust.customize(conn);
    }
    return conn.getInputStream();
  }

}
