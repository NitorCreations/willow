package com.nitorcreations.willow.servers;

import com.google.inject.servlet.ServletModule;
import com.nitorcreations.willow.servlets.PropertyServlet;

public class DeployerServletModule extends ServletModule {

  @Override
  protected void configureServlets() {
    serve("/metrics-internal/*").with(MetricsServlet.class);
    serve("/launchproperties/*").with(PropertyServlet.class);
    serve("/statistics/*").with(StatisticsServlet.class);
    serve("/poll-internal/*").with(ServerSidePollingServlet.class);
  }
}