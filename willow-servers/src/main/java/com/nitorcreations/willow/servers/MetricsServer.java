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

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.nitorcreations.logging.jetty.WebsocketRequestLog;
import com.nitorcreations.willow.protocols.Register;
 
@Named
public class MetricsServer {
  private static final Logger LOG = LoggerFactory.getLogger(MetricsServer.class);
  static {
    Register.doIt();
  }
  public static void main(final String... args) throws Exception {
    main(new MetricsServer());
  }

  public static void main(MetricsServer metrics) throws Exception {
    metrics.start(getInteger("enduserport", 5120), getInteger("deployerport", 5121));
  }
  public MetricsServer() throws Exception {
  }

  public void start(final int enduserport, final int deployerport) throws Exception {
    long start = currentTimeMillis();
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    Server server = setupServer();
    setupServerConnector(server, enduserport);
    setupServerConnector(server, deployerport);
    ClassLoader classloader =  Thread.currentThread().getContextClassLoader();
    Injector parent = Guice.createInjector(new WireModule(new MetricsServerModule(), getElasticSearchModule(),
        new SpaceModule(new URLClassSpace(classloader)))); 
    ServletContextHandler endUserContextHandler = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
    endUserContextHandler.setVirtualHosts(new String[] {"@" + enduserport });
    WillowServletContextListener euListener = getEnduserContextListener(parent);
    endUserContextHandler.addEventListener(euListener);
    FilterHolder fHolder = new FilterHolder(new LazyInitGuiceFilter(euListener));
    endUserContextHandler.addFilter(fHolder, "/*", EnumSet.allOf(DispatcherType.class));
    MimeTypes mime = endUserContextHandler.getMimeTypes();
    mime.addMimeMapping("js.gz", "text/javascript");
    mime.addMimeMapping("css.gz", "text/css");
    mime.addMimeMapping("svg.gz", "image/svg+xml");
    ServletHolder holder = endUserContextHandler.addServlet(DefaultServlet.class, "/");
    holder.setInitParameter("dirAllowed", "false");
    holder.setInitParameter("gzip", "false");
    ArrayList<String> resources = new ArrayList<>();
    for (Enumeration<URL> urls = this.getClass().getClassLoader().getResources("webapp/resource-root.marker"); urls.hasMoreElements();) {
      URL url = urls.nextElement();
      resources.add(url.toString().replace("resource-root.marker", ""));
    }
    endUserContextHandler.setBaseResource(new ResourceCollection(resources.toArray(new String[resources.size()])));
    ServletContextHandler deployerContextHandler = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
    deployerContextHandler.setVirtualHosts(new String[] {"@" + deployerport });
    WillowServletContextListener dListener = getDeployerContextListener(parent);
    deployerContextHandler.addEventListener(dListener);
    fHolder = new FilterHolder(new LazyInitGuiceFilter(dListener));
    deployerContextHandler.addFilter(fHolder, "/*", EnumSet.allOf(DispatcherType.class));

    setupHandlers(server, endUserContextHandler, deployerContextHandler);
    try {
      server.start();
      long end = currentTimeMillis();
      LOG.info("Succesfully started Jetty on ports {} (user) and {} (deployer) in {} seconds", enduserport, deployerport, (end - start) / 1000.0);
      server.join();
    } catch (Exception e) {
      LOG.info("Exception starting server", e);
    } finally {
      try {
        server.stop();
      } catch (Exception e) {
        LOG.info("Exception stopping server", e);
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
    connector.setName(String.valueOf(port));
    connector.setIdleTimeout(MINUTES.toMillis(2));
    connector.setReuseAddress(true);
    server.addConnector(connector);
  }

  private void setupHandlers(final Server server, final ServletContextHandler ... contexts) throws URISyntaxException {
    HandlerCollection handlers = new HandlerCollection();
    server.setHandler(handlers);
    for (ServletContextHandler context : contexts) {
      handlers.addHandler(context);
    }
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
  protected AbstractModule getElasticSearchModule() {
    return new ElasticSearchModule();
  }

  protected WillowServletContextListener getEnduserContextListener(Injector parent) {
    return new EndUserContextListener(parent);
  }
  protected WillowServletContextListener getDeployerContextListener(Injector parent) {
    return new DeployerContextListener(parent);
  }
}
