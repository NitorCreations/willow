package com.nitorcreations.willow.servers;

import com.google.inject.Injector;

public class MetricsProxyingServer extends MetricsServer {

  public static void main(final String... args) throws Exception {
    main(new MetricsProxyingServer());
  }

  public MetricsProxyingServer() throws Exception {
    super();
  }
  @Override
  protected WillowServletContextListener getEnduserContextListener(Injector parent) {
    return new MetricsProxyingEndUserContextListener(parent);
  }
}
