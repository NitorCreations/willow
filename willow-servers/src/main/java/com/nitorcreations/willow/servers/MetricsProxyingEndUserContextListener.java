package com.nitorcreations.willow.servers;

import com.google.inject.Injector;
import com.google.inject.servlet.ServletModule;

public class MetricsProxyingEndUserContextListener
    extends EndUserContextListener {

  public MetricsProxyingEndUserContextListener(Injector parent) {
    super(parent);
  }

  @Override
  protected ServletModule getServletModule() {
    return new MetricsProxyingEndUserServletModule();
  }
  

}
