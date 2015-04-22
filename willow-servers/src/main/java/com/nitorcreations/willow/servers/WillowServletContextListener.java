package com.nitorcreations.willow.servers;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.shiro.guice.web.ShiroWebModule;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

public class WillowServletContextListener extends GuiceServletContextListener {
  private ServletContext servletContext;

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    this.servletContext = servletContextEvent.getServletContext();
    super.contextInitialized(servletContextEvent);
  }

  @Override
  protected Injector getInjector() {
    ClassLoader classloader = getClass().getClassLoader();
    return  Guice.createInjector(
      new WireModule(new ApplicationServletModule(),
        getShiroModule(), ShiroWebModule.guiceFilterModule(),
        new SpaceModule(
          new URLClassSpace(classloader)
          )));
  }
  protected ShiroWebModule getShiroModule() {
    return new WillowShiroModule(servletContext); 
  }
}
