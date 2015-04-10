package com.nitorcreations.willow.servers;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.proxy.ProxyServlet;

@Singleton
public class ElasticsearchProxy extends ProxyServlet {
  private static final long serialVersionUID = -1295459728251583300L;

  @Override
  protected URI rewriteURI(HttpServletRequest request) {
    String path = request.getPathInfo();
    String query = request.getQueryString();
    try {
      return new URI("http", null, "localhost", 5240, path, query, null);
    } catch (URISyntaxException e) {
      return null;
    }
  }
}
