package com.nitorcreations.willow.servers;

import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.proxy.ProxyServlet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value={"SE_TRANSIENT_FIELD_NOT_RESTORED"}, justification="port set from init")
@Singleton
public class BackendProxyServlet extends ProxyServlet {
  private static final long serialVersionUID = 2325695145956031830L;
  private transient String port = "5122";
  @Override
  public void init() throws ServletException {
    super.init();
    ServletConfig config = getServletConfig();
    port = config.getInitParameter("backend.port");
    if (port == null || port.isEmpty()) port = "5122";
  }
  @Override
  protected String rewriteTarget(HttpServletRequest clientRequest)  {
    if (!validateDestination(clientRequest.getServerName(), clientRequest.getServerPort()))
      return null;

    StringBuilder target = new StringBuilder();
    target.append("http://localhost:").append(port);
    target.append(clientRequest.getServletPath()).append(clientRequest.getPathInfo());
    String query = clientRequest.getQueryString();
    if (query != null)
      target.append("?").append(query);
    return target.toString();
  }
}
