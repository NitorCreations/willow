package com.nitorcreations.willow.servers;

import org.apache.shiro.guice.web.ShiroWebModule;

import com.google.inject.Injector;
import com.google.inject.servlet.ServletModule;

public class EndUserContextListener extends WillowServletContextListener {

  public EndUserContextListener(Injector parent) {
    super(parent);
  }

  @Override
  protected ShiroWebModule getShiroModule() {
    return new EndUserShiroModule(servletContext);
  }

  @Override
  protected ServletModule getServletModule() {
    return new EndUserServletModule();
  }

}
