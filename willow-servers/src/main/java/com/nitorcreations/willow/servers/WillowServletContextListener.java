package com.nitorcreations.willow.servers;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.shiro.guice.web.ShiroWebModule;
import org.eclipse.sisu.wire.ChildWireModule;

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public abstract class WillowServletContextListener extends GuiceServletContextListener {
  protected ServletContext servletContext;
  private final Injector parent;
  private Injector child;
  public WillowServletContextListener(Injector parent) {
    this.parent = parent;
  }
  
  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    this.servletContext = servletContextEvent.getServletContext();
    super.contextInitialized(servletContextEvent);
  }
  @Override
  protected Injector getInjector() {
    return getChild();
  }
  private Injector getChild() {
    if (child == null) {
    this.child = parent.createChildInjector(new ChildWireModule(parent, getServletModule(),
        ShiroWebModule.guiceFilterModule(), getShiroModule()));
    }
    return child;
  }
  protected abstract ShiroWebModule getShiroModule();
  protected abstract ServletModule getServletModule();
}
