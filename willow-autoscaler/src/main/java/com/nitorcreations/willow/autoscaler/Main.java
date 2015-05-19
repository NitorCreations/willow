package com.nitorcreations.willow.autoscaler;


import com.google.inject.Guice;
import com.google.inject.Injector;
import com.nitorcreations.willow.autoscaler.clouds.CloudAdapters;
import com.nitorcreations.willow.autoscaler.config.AutoScalingGroupConfig;
import com.nitorcreations.willow.autoscaler.deployment.AutoScalingGroupStatus;
import com.nitorcreations.willow.autoscaler.deployment.DeploymentScanner;
import com.nitorcreations.willow.utils.MergeableProperties;
import com.nitorcreations.willow.utils.SimpleFormatter;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.*;

@Named
@Singleton
@SuppressWarnings("PMD")
public class Main {

  private Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

  private static Injector injector;

  private MergeableProperties properties = new MergeableProperties();

  @Inject
  private CloudAdapters cloudAdapters;

  @Inject
  private DeploymentScanner deploymentScanner;

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

    //query group status for all groups
    deploymentScanner.initialize(groups);

    while (true) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      AutoScalingGroupStatus status = deploymentScanner.getStatus("frontend");
      if (status != null) {
        System.out.println(status.getInstanceCount());
      } else {
        System.out.println("status null");
      }
    }
    //start polling metrics

    //make scaling decisions (profit)
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
