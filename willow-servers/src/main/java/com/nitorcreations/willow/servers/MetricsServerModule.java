package com.nitorcreations.willow.servers;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jetty.servlet.DefaultServlet;

import com.google.inject.AbstractModule;
import com.nitorcreations.willow.servlets.PropertyServlet;
import com.nitorcreations.willow.servlets.TestServlet;

public class MetricsServerModule extends AbstractModule {

  @Override
  protected void configure() {
    String env =  getProperty("env", "dev");
    setProperty("env", env);
    bind(MetricsServlet.class);
    bind(StatisticsServlet.class);
    bind(ServerSidePollingServlet.class);
    bind(HostLookupService.class).toInstance(getHostLookupService());
    bind(PropertyServlet.class).asEagerSingleton();
    bind(DefaultServlet.class).asEagerSingleton();
    bind(VelocityServlet.class).asEagerSingleton();
    bind(ExecutorService.class).toInstance(Executors.newCachedThreadPool());
    bind(ScheduledExecutorService.class).toInstance(Executors.newScheduledThreadPool(10));
    if ("dev".equals(env)) {
      bind(ElasticsearchProxy.class);
      bind(TestServlet.class).toInstance(new TestServlet());
    }

  }
  protected HostLookupService getHostLookupService() {
    String hlsClassName = getProperty("willow.hostLookupService");
    if (hlsClassName == null) {
      return new SimpleHostLookupService();
    }
    try {
      Class<?> hlsClass = Class.forName(hlsClassName);
      return (HostLookupService)hlsClass.newInstance();
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Unable to instantiate host lookupservice", e);
    }
  }

}
