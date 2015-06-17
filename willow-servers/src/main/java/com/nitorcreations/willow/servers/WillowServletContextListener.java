package com.nitorcreations.willow.servers;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.shiro.guice.web.ShiroWebModule;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

public class WillowServletContextListener extends GuiceServletContextListener {
  protected ServletContext servletContext;

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    this.servletContext = servletContextEvent.getServletContext();
    super.contextInitialized(servletContextEvent);
  }

  @Override
  protected Injector getInjector() {
    ClassLoader classloader =  Thread.currentThread().getContextClassLoader();
    return  Guice.createInjector(
        new WireModule(new ApplicationServletModule(),
            getShiroModule(), ShiroWebModule.guiceFilterModule(),
            getElasticSearchModule(),
            new SpaceModule(
                new URLClassSpace(classloader)
                )));
  }
  protected ShiroWebModule getShiroModule() {
    return new WillowShiroModule(servletContext);
  }
  protected AbstractModule getElasticSearchModule() {
    return new ElasticSearchModule();
  }
}
