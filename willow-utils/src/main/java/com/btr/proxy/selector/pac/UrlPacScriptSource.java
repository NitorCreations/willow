package com.btr.proxy.selector.pac;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/*****************************************************************************
 * Script source that will load the content of a PAC file from an webserver. The
 * script content is cached once it was downloaded.
 *
 * @author Bernd Rosstauscher (proxyvole@rosstauscher.de) Copyright 2009
 ****************************************************************************/

public class UrlPacScriptSource implements PacScriptSource {
  private final Logger log = Logger.getLogger(UrlPacScriptSource.class.getCanonicalName());

  private final String scriptUrl;
  private String scriptContent;
  private long expireAtMillis;

  /*************************************************************************
   * Constructor
   *
   * @param url
   *          the URL to download the script from.
   ************************************************************************/

  public UrlPacScriptSource(String url) {
    super();
    this.expireAtMillis = 0;
    this.scriptUrl = url;
  }

  /*************************************************************************
   * getScriptContent
   *
   * @see com.btr.proxy.selector.pac.PacScriptSource#getScriptContent()
   ************************************************************************/

  @Override
  public synchronized String getScriptContent() throws IOException {
    if (this.scriptContent == null || this.expireAtMillis > 0
        && this.expireAtMillis < System.currentTimeMillis()) {
      ProxySelector old = ProxySelector.getDefault();
      try {
        // Reset it again with next download we should get a new expire info
        this.expireAtMillis = 0;
        ProxySelector.setDefault(new ProxySelector() {
          @Override
          public List<Proxy> select(URI uri) {
            return Arrays.asList(Proxy.NO_PROXY);
          }
          @Override
          public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
          }
        });
        this.scriptContent = readPacFileContent(this.scriptUrl);
      } finally {
        ProxySelector.setDefault(old);
      }
    }
    return this.scriptContent;
  }

  /*************************************************************************
   * Reads a PAC script from a local file.
   *
   * @param scriptUrl
   * @return the content of the script file.
   * @throws IOException
   * @throws URISyntaxException
   ************************************************************************/
  private String readPacFileContent(String scriptUrl) throws IOException {
    try (InputStream in = new URL(scriptUrl).openConnection(Proxy.NO_PROXY).getInputStream();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      StringBuilder result = new StringBuilder();
      try {
        String line;
        while ((line = r.readLine()) != null) {
          result.append(line).append("\n");
        }
      } finally {
        r.close();
      }
      return result.toString();
    }
  }

  /***************************************************************************
   * @see java.lang.Object#toString()
   **************************************************************************/
  @Override
  public String toString() {
    return this.scriptUrl;
  }

  /*************************************************************************
   * isScriptValid
   *
   * @see com.btr.proxy.selector.pac.PacScriptSource#isScriptValid()
   ************************************************************************/

  @Override
  public boolean isScriptValid() {
    try {
      String script = getScriptContent();
      if (script.trim().length() == 0) {
        log.log(Level.FINE, "PAC script is empty. Skipping script!");
        return false;
      }
      if (script.indexOf("FindProxyForURL") == -1) {
        log.log(Level.FINE,
            "PAC script entry point FindProxyForURL not found. Skipping script!");
        return false;
      }
      return true;
    } catch (IOException e) {
      log.log(Level.FINE, "File reading error", e);
      return false;
    }
  }

}
