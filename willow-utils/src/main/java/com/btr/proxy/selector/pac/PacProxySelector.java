package com.btr.proxy.selector.pac;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/*****************************************************************************
 * ProxySelector that will use a PAC script to find an proxy for a given URI.
 *
 * @author Bernd Rosstauscher (proxyvole@rosstauscher.de) Copyright 2009
 ****************************************************************************/
public class PacProxySelector extends ProxySelector {
  protected static final Logger log = Logger.getLogger(PacProxySelector.class
      .getCanonicalName());

  private final static int PROXY_TYPE_LEN = 6;
  private static final String PAC_PROXY = "PROXY";
  private static final String PAC_SOCKS = "SOCKS";
  private static final String PAC_DIRECT = "DIRECT";

  private PacScriptParser pacScriptParser;

  private static volatile boolean enabled = true;

  /*************************************************************************
   * Constructor
   *
   * @param pacSource
   *          the source for the PAC file.
   ************************************************************************/

  public PacProxySelector(PacScriptSource pacSource) {
    super();
    selectEngine(pacSource);
  }

  /*************************************************************************
   * Can be used to enable / disable the proxy selector. If disabled it will
   * return DIRECT for all urls.
   *
   * @param enable
   *          the new status to set.
   ************************************************************************/

  public static void setEnabled(boolean enable) {
    enabled = enable;
  }

  /*************************************************************************
   * Checks if the selector is currently enabled.
   *
   * @return true if enabled else false.
   ************************************************************************/

  public static boolean isEnabled() {
    return enabled;
  }

  /*************************************************************************
   * Selects one of the available PAC parser engines.
   *
   * @param pacSource
   *          to use as input.
   ************************************************************************/

  private void selectEngine(PacScriptSource pacSource) {
    try {
      log.log(Level.INFO, "Using javax.script JavaScript engine.");
      this.pacScriptParser = new JavaxPacScriptParser(pacSource);
    } catch (Exception e) {
      log.log(Level.INFO, "PAC parser error.", e);
    }
  }

  /*************************************************************************
   * connectFailed
   *
   * @see java.net.ProxySelector#connectFailed(java.net.URI,
   *      java.net.SocketAddress, java.io.IOException)
   ************************************************************************/
  @Override
  public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
    // Not used.
  }

  /*************************************************************************
   * select
   *
   * @see java.net.ProxySelector#select(java.net.URI)
   ************************************************************************/
  @Override
  public List<Proxy> select(URI uri) {
    if (uri == null) {
      throw new IllegalArgumentException("URI must not be null.");
    }

    // Fix for Java 1.6.16+ where we get a infinite loop because
    // URL.connect(Proxy.NO_PROXY) does not work as expected.
    if (!enabled) {
      return noProxyList();
    }

    return findProxy(uri);
  }

  /*************************************************************************
   * Evaluation of the given URL with the PAC-file.
   *
   * Two cases can be handled here: DIRECT Fetch the object directly from the
   * content HTTP server denoted by its URL PROXY name:port Fetch the object via
   * the proxy HTTP server at the given location (name and port)
   *
   * @param uri
   *          <code>URI</code> to be evaluated.
   * @return <code>Proxy</code>-object list as result of the evaluation.
   ************************************************************************/

  private List<Proxy> findProxy(URI uri) {
    try {
      List<Proxy> proxies = new ArrayList<Proxy>();
      String parseResult = this.pacScriptParser.evaluate(uri.toString(),
          uri.getHost());
      String[] proxyDefinitions = parseResult.split("[;]");
      for (String proxyDef : proxyDefinitions) {
        if (proxyDef.trim().length() > 0) {
          proxies.add(buildProxyFromPacResult(proxyDef));
        }
      }
      return proxies;
    } catch (ProxyEvaluationException e) {
      log.log(Level.INFO, "PAC resolving error.", e);
      return noProxyList();
    }
  }

  /*************************************************************************
   * The proxy evaluator will return a proxy string. This method will take this
   * string and build a matching <code>Proxy</code> for it.
   *
   * @param pacResult
   *          the result from the PAC parser.
   * @return a Proxy
   ************************************************************************/
  public static Proxy buildProxyFromPacResult(String pacResult) {
    if (pacResult == null || pacResult.trim().length() < PROXY_TYPE_LEN) {
      return Proxy.NO_PROXY;
    }
    String proxyDef = pacResult.trim();
    if (proxyDef.toUpperCase(Locale.ENGLISH).startsWith(PAC_DIRECT)) {
      return Proxy.NO_PROXY;
    }

    // Check proxy type.
    Proxy.Type type = Proxy.Type.HTTP;
    if (proxyDef.toUpperCase(Locale.ENGLISH).startsWith(PAC_SOCKS)) {
      type = Proxy.Type.SOCKS;
    } else if (!proxyDef.toUpperCase(Locale.ENGLISH).startsWith(PAC_PROXY)) {
      return Proxy.NO_PROXY;
    }

    String host = proxyDef.substring(PROXY_TYPE_LEN);
    Integer port = DEFAULT_PROXY_PORT;

    // Split port from host
    int indexOfPort = host.indexOf(':');
    if (indexOfPort != -1) {
      port = Integer.parseInt(host.substring(indexOfPort + 1).trim());
      host = host.substring(0, indexOfPort).trim();
    }

    SocketAddress adr = InetSocketAddress.createUnresolved(host, port);
    return new Proxy(type, adr);
  }

  public static final int DEFAULT_PROXY_PORT = 80;

  private static List<Proxy> noProxyList;

  /*************************************************************************
   * Gets an unmodifiable proxy list that will have as it's only entry an DIRECT
   * proxy.
   *
   * @return a list with a DIRECT proxy in it.
   ************************************************************************/

  public static synchronized List<Proxy> noProxyList() {
    if (noProxyList == null) {
      ArrayList<Proxy> list = new ArrayList<Proxy>(1);
      list.add(Proxy.NO_PROXY);
      noProxyList = Collections.unmodifiableList(list);
    }
    return noProxyList;
  }

  /*************************************************************************
   * Build a PAC proxy selector for the given URL.
   *
   * @param url
   *          to fetch the PAC script from.
   * @return a PacProxySelector or null if it is not possible to build a working
   *         selector.
   ************************************************************************/

  public static PacProxySelector buildPacSelectorForUrl(String url) {
    PacProxySelector result = null;
    PacScriptSource pacSource = new UrlPacScriptSource(url);
    if (pacSource.isScriptValid()) {
      result = new PacProxySelector(pacSource);
    }
    return result;
  }

}
