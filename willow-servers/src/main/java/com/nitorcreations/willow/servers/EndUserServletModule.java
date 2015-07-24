package com.nitorcreations.willow.servers;

import static java.lang.System.getProperty;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.servlet.DefaultServlet;

import com.google.inject.servlet.ServletModule;
import com.nitorcreations.willow.servlets.PropertyServlet;
import com.nitorcreations.willow.servlets.TestServlet;

public class EndUserServletModule extends ServletModule {

  @Override
  protected void configureServlets() {
    Map<String, String> defaultInit = new HashMap<>();
    defaultInit.put("dirAllowed", "false");
    defaultInit.put("gzip", "true");
    defaultInit.put("maxCacheSize","81920");
    defaultInit.put("maxCacheSize","81920");
    defaultInit.put("welcomeServlets", "true");
    String env =  getProperty("env", "dev");
    if ("dev".equals(env)) {
      serve("/test/*").with(TestServlet.class);
      serve("/search/*").with(ElasticsearchProxy.class);
      defaultInit.put("gzip", "false");
    }
    serve("/metrics/*").with(MetricsServlet.class);
    serve("/properties/*").with(PropertyServlet.class);
    serve("/launchproperties/*").with(PropertyServlet.class);
    serve("/statistics/*").with(StatisticsServlet.class);
    serve("/rawterminal/*").with(RawTerminalServlet.class);
    serve("/session/*").with(SessionServlet.class);
    serve("/poll/*").with(ServerSidePollingServlet.class);
    serve("/poll-internal/*").with(ServerSidePollingServlet.class);
    serve("*.html").with(VelocityServlet.class);
    serve("/*").with(DefaultServlet.class, defaultInit);
  }
}