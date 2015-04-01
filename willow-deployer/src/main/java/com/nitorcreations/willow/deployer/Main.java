package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DEPLOYER_LAUNCH_INDEX;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_LAUNCH_URLS;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_METHOD;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_LAUNCH;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_POST_DOWNLOAD;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_POST_STOP_OLD;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_PRE_DOWNLOAD;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_PRE_START;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_SHUTDOWN;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PROPERTIES_FILENAME;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_STATISTICS_FLUSHINTERVAL;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_STATISTICS;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_STATISTICS_URI;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_WORKDIR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.inject.Inject;
import javax.inject.Named;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.nitorcreations.willow.messages.WebSocketTransmitter;
import com.nitorcreations.willow.utils.MergeableProperties;

@Named
public class Main extends DeployerControl implements MainMBean {
  private List<PlatformStatsSender> stats = new ArrayList<>();
  private List<LaunchMethod> children = new ArrayList<>();
  private List<StatisticsSender> statistics = new ArrayList<>();
  private static AtomicReference<String> statisticsUrl = new AtomicReference<>(null);
  private static AtomicLong statisticsInterval = new AtomicLong(2000);
  
  @Inject
  private Map<String, StatisticsSender> statisticSenders;
  
  @Inject 
  private WebSocketTransmitter transmitter;
  
  public Main() {}

  private void registerBean() {
    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    try {
      mbs.registerMBean(this, OBJECT_NAME);
    } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
      e.printStackTrace();
    }
  }
 
  public static void main(String[] args) throws URISyntaxException {
    injector.getInstance(Main.class).doMain(args);
  }

  public void doMain(String[] args) {
    if (args.length < 2)
      usage("At least two arguments expected: {name} {launch.properties}");
    populateProperties(args);
    MergeableProperties mergedProperties = new MergeableProperties();
    for (int i = launchPropertiesList.size() - 1; i >= 0; i--) {
      mergedProperties.putAll(launchPropertiesList.get(i));
    }
    List<String> launchUrls = mergedProperties.getArrayProperty(PROPERTY_KEY_LAUNCH_URLS);
    if (launchUrls.size() > 0) {
      launchUrls.add(0, deployerName);
      new Main().doMain(launchUrls.toArray(new String[launchUrls.size()]));
      return;
    }
    extractNativeLib();
    registerBean();
    String statUri = mergedProperties.getProperty(PROPERTY_KEY_STATISTICS_URI);
    if (statUri != null && !statUri.isEmpty()) {
      try {
        long flushInterval = Long.parseLong(mergedProperties.getProperty(PROPERTY_KEY_STATISTICS_FLUSHINTERVAL, "5000"));
        transmitter.setFlushInterval(flushInterval);
        transmitter.setUri(new URI(statUri));
        transmitter.start();
      } catch (URISyntaxException e) {
        usage(e);
      }
    }
      List<String> stats;
      if (mergedProperties.getProperty(PROPERTY_KEY_PREFIX_STATISTICS + "[0]") == null) {
        stats = Arrays.asList("platform", "process");
      } else {
        stats = mergedProperties.getArrayProperty(PROPERTY_KEY_PREFIX_STATISTICS);
      }
      for (int i=0; i<stats.size(); i++) {
        StatisticsSender nextStat = statisticSenders.get(stats.get(i));
        if (nextStat != null) {
          try {
            nextStat = nextStat.getClass().newInstance();
            injector.injectMembers(nextStat);
            nextStat.setProperties(mergedProperties.getPrefixed(PROPERTY_KEY_PREFIX_STATISTICS + "[" + i + "]"));
            statistics.add(nextStat);
          } catch (InstantiationException | IllegalAccessException e) {
          }
        }
      }
    // Download
    try {
      runHooks(PROPERTY_KEY_PREFIX_PRE_DOWNLOAD, launchPropertiesList, true);
    } catch (Exception e) {
      usage(e);
    }
    download();
    try {
      runHooks(PROPERTY_KEY_PREFIX_POST_DOWNLOAD, launchPropertiesList, true);
    } catch (Exception e) {
      usage(e);
    }
    // Stop
    stopOld(args);
    try {
      runHooks(PROPERTY_KEY_PREFIX_POST_STOP_OLD, launchPropertiesList, true);
    } catch (Exception e) {
      usage(e);
    }
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        Main.this.stop();
      }
    });
    // Start
    int i = 0;
    for (MergeableProperties launchProps : launchPropertiesList) {
      LaunchMethod launcher = null;
      try {
        String method = launchProps.getProperty(PROPERTY_KEY_PREFIX_LAUNCH + PROPERTY_KEY_METHOD);
        if (method != null && LaunchMethod.TYPE.valueOf(method) != null) {
          launcher = LaunchMethod.TYPE.valueOf(method).getLauncher();
        }
      } catch (Throwable t) {
        usage(t);
      }
      launchProps.setProperty(PROPERTY_KEY_DEPLOYER_LAUNCH_INDEX, Integer.toString(i));
      File workDir = new File(launchProps.getProperty(PROPERTY_KEY_WORKDIR, "."));
      String propsName = launchProps.getProperty(PROPERTY_KEY_PROPERTIES_FILENAME, "application.properties");
      File propsFile = new File(propsName);
      if (!propsFile.isAbsolute()) {
        propsFile = new File(workDir, propsName);
      }
      File propsDir = propsFile.getParentFile();
      if (!FileUtil.createDir(propsDir)) {
        usage("Unable to create properties directory " + workDir.getAbsolutePath());
      }
      if (!FileUtil.createDir(workDir)) {
        usage("Unable to create work directory " + workDir.getAbsolutePath());
      }
      try {
        launchProps.store(new FileOutputStream(propsFile), null);
      } catch (IOException e) {
        usage(e);
      }
      try {
        Main.runHooks(PROPERTY_KEY_PREFIX_PRE_START, Collections.singletonList(launchProps), true);
      } catch (Exception e) {
        usage(e);
      }
      if (launcher != null) {
        launcher.setProperties(launchProps);
        executor.submit(launcher);
        children.add(launcher);
      }
      i++;
    }
    if (statistics.isEmpty() && children.isEmpty()) {
      System.exit(0);
    }
  }

  public void stop() {
    for (LaunchMethod next : children) {
      next.stopRelaunching();
    }
    try {
      Main.runHooks(PROPERTY_KEY_PREFIX_SHUTDOWN, launchPropertiesList, false);
    } catch (Exception e) {
      LogRecord rec = new LogRecord(Level.SEVERE, "Shutdown failed");
      rec.setThrown(e);
      log.log(rec);
    }
    if (children.size() > 0) {
      final ExecutorService stopexec = Executors.newFixedThreadPool(children.size());
      List<Future<Integer>> waits = new ArrayList<>();
      for (final LaunchMethod next : children) {
        waits.add(stopexec.submit(new Callable<Integer>() {
          @Override
          public Integer call() throws Exception {
            return next.destroyChild();
          }
        }));
      }
      int i = 0;
      for (Future<Integer> next : waits) {
        try {
          log.info("Child " + i + " returned " + next.get());
        } catch (InterruptedException | ExecutionException e) {
          log.warning("Destroy failed: " + e.getMessage());
        }
      }
      stopexec.shutdownNow();
    }
    for (PlatformStatsSender next : stats) {
      next.stop();
    }
    executor.shutdownNow();
  }

  @Override
  public String getStatus() {
    StringBuilder ret = new StringBuilder(deployerName).append(" running ");
    if (children.size() == 1) {
      ret.append("1 child (" + children.get(0).getName() + ": ").append(children.get(0).getProcessId()).append(" - restarts: ").append(children.get(0).restarts()).append(")");
    } else {
      ret.append(children.size()).append(" children ");
      for (LaunchMethod next : children) {
        ret.append("(" + next.getName() + ": ").append(next.getProcessId()).append(" - restarts:").append(next.restarts()).append(") ");
      }
      ret.setLength(ret.length() - 1);
    }
    return ret.toString();
  }

  @Override
  protected void usage(String message) {
    stop();
    super.usage(message);
  }

  protected void usage(Throwable e) {
    stop();
    e.printStackTrace();
    super.usage(e.getMessage());
  }
  public static String getStatisticsUrl() {
    return statisticsUrl.get();
  }
  public static long getStatisticsInterval() {
    return statisticsInterval.get();
  }
  @Override
  public String[] getChildNames() {
    ArrayList<String> ret = new ArrayList<>();
    for (LaunchMethod next : children) {
      ret.add(next.getName());
    }
    return ret.toArray(new String[ret.size()]);
  }

  @Override
  public long getChildPid(String childName) {
    if (childName == null || childName.isEmpty()) return -1;
    for (LaunchMethod next : children) {
      if (childName.equals(next.getName())) {
        return next.getProcessId();
      }
    }
    return -1;
  }
  @Override
  public void restartChild(String childName) {
    for (LaunchMethod next : children) {
      if (childName == null || childName.isEmpty() || childName.equals(next.getName())) {
        try {
          next.destroyChild();
        } catch (InterruptedException e) {
          LogRecord rec = new LogRecord(Level.INFO, "Failed to restart child " + next.getName());
          rec.setThrown(e);
          log.log(rec);
        }
      }
    }
  }
}
