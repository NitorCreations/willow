package com.nitorcreations.willow.servers;

import org.apache.shiro.guice.web.ShiroWebModule;

import com.google.inject.Injector;
import com.google.inject.servlet.ServletModule;

public class DeployerContextListener extends WillowServletContextListener {

  public DeployerContextListener(Injector parent) {
    super(parent);
  }

  @Override
  protected ShiroWebModule getShiroModule() {
    return new DeployerShiroModule(servletContext);
  }

  @Override
  protected ServletModule getServletModule() {
     return new DeployerServletModule();
  }

}
