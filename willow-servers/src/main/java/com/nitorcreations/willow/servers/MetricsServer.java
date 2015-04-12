package com.nitorcreations.willow.servers;

import static java.lang.Integer.getInteger;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;

import javax.inject.Named;
import javax.servlet.DispatcherType;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.nitorcreations.logging.jetty.WebsocketRequestLog;

@Named
public class MetricsServer {
  private static final Logger LOG = LoggerFactory.getLogger(MetricsServer.class);

  public static void main(final String... args) throws Exception {
    ClassLoader classloader = MetricsServer.class.getClassLoader();
    Injector injector = Guice.createInjector(
      new WireModule(new ApplicationServletModule(),
        new SpaceModule(
          new URLClassSpace(classloader)
          )));
    injector.getInstance(MetricsServer.class).start(getInteger("port", 5120));
  }

  public MetricsServer() throws Exception {
  }

  public void start(final int port) throws Exception {
    long start = currentTimeMillis();
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    Server server = setupServer();
    setupServerConnector(server, port);
    ServletContextHandler servletContextHandler = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
    servletContextHandler.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
    ServletHolder holder = servletContextHandler.addServlet(DefaultServlet.class, "/");
    holder.setInitParameter("dirAllowed", "false");
    holder.setInitParameter("gzip", "false");
    ArrayList<String> resources = new ArrayList<>();
    for (Enumeration<URL> urls = this.getClass().getClassLoader().getResources("resource-root.marker"); urls.hasMoreElements();) {
      URL url = urls.nextElement();
      resources.add(url.toString().replace("resource-root.marker", ""));
    }
    servletContextHandler.setBaseResource(new ResourceCollection(resources.toArray(new String[resources.size()])));
    setupHandlers(server, servletContextHandler);
    try {
      server.start();
      long end = currentTimeMillis();
      LOG.info("Succesfully started Jetty on port {} in {} seconds", port, (end - start) / 1000.0);
      server.join();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        server.stop();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }    
  }

  private Server setupServer() {
    Server server = new Server(new QueuedThreadPool(100));
    server.setStopAtShutdown(true);
    MBeanContainer mbContainer = new MBeanContainer(
      ManagementFactory.getPlatformMBeanServer());
    server.addBean(mbContainer);
    return server;
  }

  private void setupServerConnector(final Server server, final int port) {
    ServerConnector connector = new ServerConnector(server);
    connector.setPort(port);
    connector.setIdleTimeout(MINUTES.toMillis(2));
    connector.setReuseAddress(true);
    server.addConnector(connector);
  }

  private void setupHandlers(final Server server, final ServletContextHandler context) throws URISyntaxException {
    HandlerCollection handlers = new HandlerCollection();
    server.setHandler(handlers);
    handlers.addHandler(context);
    handlers.addHandler(createAccessLogHandler());
  }

  private RequestLogHandler createAccessLogHandler() throws URISyntaxException {
    RequestLogHandler requestLogHandler = new RequestLogHandler();
    if (System.getProperty("wslogging.url") != null) {
      WebsocketRequestLog requestLog = new WebsocketRequestLog(2000, System.getProperty("wslogging.url"));
      requestLogHandler.setRequestLog(requestLog);
      requestLog.setPreferProxiedForAddress(true);
    } else {
      NCSARequestLog requestLog = new NCSARequestLog("yyyy_mm_dd.request.log");
      requestLog.setRetainDays(90);
      requestLog.setAppend(true);
      requestLog.setExtended(true);
      requestLog.setLogTimeZone("Europe/Helsinki");
      requestLog.setPreferProxiedForAddress(true);
      requestLog.setLogLatency(true);
      requestLogHandler.setRequestLog(requestLog);
    }
    return requestLogHandler;
  }
}
