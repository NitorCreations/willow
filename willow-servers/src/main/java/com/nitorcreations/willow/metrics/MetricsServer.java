package com.nitorcreations.willow.metrics;

import static com.nitorcreations.core.utils.KillProcess.killProcessUsingPort;
import static java.lang.Integer.getInteger;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SECURITY;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import ch.qos.logback.classic.LoggerContext;

public class MetricsServer {
    final LoggerContext factory = (LoggerContext) LoggerFactory.getILoggerFactory();
    
    private static final Logger LOG = LoggerFactory.getLogger(MetricsServer.class);
    public static void main(final String... args) throws Exception {
        new MetricsServer().start(getInteger("port", 5120), getProperty("env", "dev"), getProperty("static.resources", ""));
    }

    public Server start(final int port, final String env, String staticResources) throws Exception {
        long start = currentTimeMillis();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        setProperty("env", env);
        killProcessUsingPort(port);
        Server server = setupServer();
        setupServerConnector(server, port);
        ServletContextHandler context = setupServletContextHandler();
        setupResourceBases(context, "terminal-resources", "metrics-resources");
        setupMetrics(context);
        setupProperties(context);
        setupStatistics(context);
        setupTerminal(context);
        setupProxy(context);
        setupHandlers(server, context);
        server.start();
        long end = currentTimeMillis();
        LOG.info("Succesfully started Jetty on port {} in {} seconds in environment {}", port, (end - start) / 1000.0, env);
        LOG.info("Web site available at http://localhost:" + port + "/");
        LOG.info("REST API available at http://localhost:" + port + "/rest");
        LOG.info("Metrics and health checks available at http://localhost:" + port + "/metrics");
        return server;
    }
    
    private void setupResourceBases(final ServletContextHandler context, String ... resourceBases) throws IOException {
    	List<String> resources = new ArrayList<>();
    	for (String next : resourceBases) {
    		int i=1;
    		String index = next + "/index.html";
    		for (Enumeration<URL> urls = this.getClass().getClassLoader().getResources(index); urls.hasMoreElements();) {
    			URL url = urls.nextElement();
    			resources.add(url.toString().replace(index, ""));
    		}
    		LOG.info("Resources included from : " + resources.toString());
    		context.setBaseResource(new ResourceCollection(resources.toArray(new String[resources.size()])));
    		ServletHolder holder = context.addServlet(DefaultServlet.class, "/" + next + "/*");
    		holder.setInitParameter("dirAllowed", "false");
    		holder.setInitParameter("gzip", "false");
    		holder.setDisplayName("terminal-resources");
    		holder.setInitOrder(i++);
    	}
    }


    private void setupProxy(ServletContextHandler context) {
    	context.addServlet(ElasticsearchProxy.class, "/search/*");
	}

    private Server setupServer() {
        Server server = new Server(new QueuedThreadPool(100));
        server.setStopAtShutdown(true);
        return server;
    }

    private void setupServerConnector(final Server server, final int port) {
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        connector.setIdleTimeout(MINUTES.toMillis(2));
        connector.setReuseAddress(true);
        server.addConnector(connector);
    }

    private ServletContextHandler setupServletContextHandler() {
        ServletContextHandler context = new ServletContextHandler(NO_SESSIONS | NO_SECURITY);
        context.setDisplayName("metrics");
        context.setStopTimeout(SECONDS.toMillis(10));
        return context;
    }

    private void setupHandlers(final Server server, final ServletContextHandler context) throws URISyntaxException {
        HandlerCollection handlers = new HandlerCollection();
        server.setHandler(handlers);
        handlers.addHandler(context);
        handlers.addHandler(createAccessLogHandler());
    }


    private void setupMetrics(final ServletContextHandler context) {
        ServletHolder holder = context.addServlet(MetricsServlet.class, "/metrics/*");
        holder.setInitOrder(1);
    }
    private void setupProperties(final ServletContextHandler context) {
        ServletHolder holder = context.addServlet(PropertiesServlet.class, "/properties/*");
        holder.setInitOrder(2);
    }

    private void setupStatistics(final ServletContextHandler context) {
        LOG.info("Enable statistics servlet");
    	context.addServlet(StatisticsServlet.class, "/statistics/*");
    }

    private void setupTerminal(final ServletContextHandler context) {
        LOG.info("Enable statistics servlet");
    	context.addServlet(TerminalServlet.class, "/terminal/*");
    }
    
    private RequestLogHandler createAccessLogHandler() throws URISyntaxException {
        RequestLogHandler requestLogHandler = new RequestLogHandler();
    	if (System.getProperty("accesslog.websocket") != null) {
    		WebsocketRequestLog requestLog = new WebsocketRequestLog(2000, System.getProperty("accesslog.websocket"));
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
