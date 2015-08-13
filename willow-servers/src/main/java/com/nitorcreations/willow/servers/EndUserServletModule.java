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
    configureHealthCheck();
    configureMetrics();
    configureProperties();
    configureRawTerminal();
    configureSession();
    configurePoll();
    configureVelocity();
    configureDefault();
  }
  protected void configureHealthCheck() {
    serve("/healthcheck/*").with(InfoServlet.class);
  }
  protected void configureMetrics() {
    serve("/metrics/*").with(MetricsServlet.class);
  }
  protected void configureProperties() {
    serve("/properties/*").with(PropertyServlet.class);
  }
  protected void configureRawTerminal() {
    serve("/rawterminal/*").with(RawTerminalServlet.class);
  }
  protected void configureSession() {
    serve("/session/*").with(SessionServlet.class);
  }
  protected void configurePoll() {
    serve("/poll/*").with(ServerSidePollingServlet.class);
  }
  protected void configureVelocity() {
    serve("*.html").with(VelocityServlet.class);
  }
  protected void configureDefault() {
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
    serve("/*").with(DefaultServlet.class, defaultInit);
  }
}