package com.nitorcreations.willow.autoscaler;


import com.google.inject.Guice;
import com.google.inject.Injector;
import com.nitorcreations.willow.autoscaler.config.AutoScalingGroupConfig;
import com.nitorcreations.willow.autoscaler.deployment.DeploymentScanner;
import com.nitorcreations.willow.autoscaler.metrics.AutoScalingStatus;
import com.nitorcreations.willow.autoscaler.metrics.MetricPoller;
import com.nitorcreations.willow.autoscaler.scaling.Scaler;
import com.nitorcreations.willow.messages.WebSocketTransmitter;
import com.nitorcreations.willow.utils.MergeableProperties;
import com.nitorcreations.willow.utils.SimpleFormatter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.*;

@Named
@Singleton
@SuppressFBWarnings(value={"DM_EXIT"}, justification="cli tool needs to convey correct exit code")
public class Main {

  private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

  private static Injector injector;

  private MergeableProperties properties = new MergeableProperties();

  @Inject
  ExecutorService executorService;

  @Inject
  private DeploymentScanner deploymentScanner;

  @Inject
  private MetricPoller metricPoller;

  @Inject
  private AutoScalingStatus autoScalingStatus;

  @Inject
  private Scaler scaler;

  @Inject
  private WebSocketTransmitter messageTransmitter;

  static {
    try {
      setupLogging();
      injector = Guice.createInjector(
          new WireModule(
              new AutoScalerModule(),
              new SpaceModule(
                  new URLClassSpace(Main.class.getClassLoader())
          )));
    } catch (Throwable e) {
      e.printStackTrace();
      assert false;
    }
  }

  public static void main(String... args) {
    injector.getInstance(Main.class).doMain(args);
  }

  public void doMain(String... args) {
    for (String arg : args) {
      properties = new MergeableProperties().merge(System.getProperties(), arg);
    }
    //read groups from configuration
    MergeableProperties allGroupProperties = properties.getPrefixed("willow-autoscaler.groups");
    int i = 0;
    MergeableProperties groupProperties;
    List<AutoScalingGroupConfig> groups = new LinkedList<>();
    while (!(groupProperties = allGroupProperties.getPrefixed("[" + i++ + "]")).isEmpty()) {
      groups.add(AutoScalingGroupConfig.fromProperties(groupProperties));
    }

    if (groups.isEmpty()) {
      logger.info("No auto scaling groups configured. Exiting.");
      System.exit(0);
    }

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        Main.this.stop();
      }
    });

    //initialize message transmitter
    try {
      messageTransmitter.setUri(new URI(properties.getProperty("willow-autoscaler.messagesUri")));
    } catch (URISyntaxException e) {
      logger.log(Level.SEVERE, "Invalid message websocket URI", e);
      System.exit(1);
    }
    messageTransmitter.start();

    autoScalingStatus.initialize(groups);

    //start scanning deployment for auto scaling groups
    logger.info("Initializing deployment scanner");
    deploymentScanner.initialize(groups);
    Future<?> future = executorService.submit(deploymentScanner);
    if (!future.isDone()) {
      logger.info("Initialized deployment scanner");
    }
    //start polling metrics
    try {
      logger.info("Initializing metrics poller");
      metricPoller.initialize(groups, new URI((String)properties.get("willow-autoscaler.metricsUri")));
    } catch (URISyntaxException e) {
      logger.log(Level.SEVERE, "Invalid URI: " + properties.get("willow-autoscaler.metricsUri"), e);
      System.exit(1);
    }

    //start scaler component
    logger.info("Initializing scaler");
    scaler.initialize(groups);

  }

  private void stop() {
    System.out.println("Willow Autoscaler stopping...");
    executorService.shutdown();
    deploymentScanner.stop();
    messageTransmitter.stop();
    System.out.println("Willow Autoscaler done stopping.");
  }

  private static void setupLogging() {
    Logger rootLogger = Logger.getLogger("");
    for (Handler nextHandler : rootLogger.getHandlers()) {
      rootLogger.removeHandler(nextHandler);
    }
    Handler console = new ConsoleHandler();
    console.setLevel(Level.INFO);
    console.setFormatter(new SimpleFormatter());
    rootLogger.addHandler(console);
    rootLogger.setLevel(Level.INFO);
    console.setFilter(new Filter() {
      @Override
      public boolean isLoggable(LogRecord record) {
        return record.getLoggerName() == null || !record.getLoggerName().startsWith("org.eclipse.jetty.util.log");
      }
    });
  }
}
