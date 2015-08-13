package com.nitorcreations.willow.servers;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.proxy.ProxyServlet;

public class MetricsProxyingEndUserServletModule extends EndUserServletModule {
  @Override
  protected void configureServlets() {
    bind(MetricsProxyServlet.class).asEagerSingleton();
    super.configureServlets();
  }
  @Override
  protected void configureMetrics() {
    serve("/metrics/*").with(MetricsProxyServlet.class);
  }
  public static class MetricsProxyServlet extends ProxyServlet {
    private transient Logger log = Logger.getLogger(getClass().getName());
    private static final long serialVersionUID = 2110808412869526116L;
    private final String prefix;
    public MetricsProxyServlet() {
      prefix = System.getProperty("willow.proxy.metrics");
    }
    @Override
    protected String rewriteTarget(HttpServletRequest clientRequest) {
      StringBuilder ret = new StringBuilder(prefix);
      ret.append(clientRequest.getPathInfo());
      String query = clientRequest.getQueryString();
      if (query != null) {
        ret.append("?").append(query);
      }
      String retVal = ret.toString();
      log.fine("Redirecting to " + retVal);
      return retVal;
    }
  }
}
