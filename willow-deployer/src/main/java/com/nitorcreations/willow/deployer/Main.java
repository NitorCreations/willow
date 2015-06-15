package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DEPLOYER_LAUNCH_INDEX;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DEPLOYER_NAME;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DOWNLOAD_DIRECTORY;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DOWNLOAD_RETRIES;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_LAUNCH_URLS;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_DOWNLOAD_URL;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_LAUNCH;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_POST_DOWNLOAD;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_POST_START;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_POST_STOP;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_POST_STOP_OLD;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_PRE_DOWNLOAD;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_PRE_START;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_SHUTDOWN;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_STATISTICS;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PROPERTIES_FILENAME;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_REMOTE_REPOSITORY;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_STATISTICS_FLUSHINTERVAL;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_STATISTICS_URI;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRA_ENV_KEYS;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_METHOD;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_TIMEOUT;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessQuery;
import org.hyperic.sigar.ptql.ProcessQueryFactory;

import com.nitorcreations.willow.deployer.download.FileUtil;
import com.nitorcreations.willow.deployer.download.PreLaunchDownloadAndExtract;
import com.nitorcreations.willow.deployer.launch.LaunchCallback;
import com.nitorcreations.willow.deployer.launch.LaunchMethod;
import com.nitorcreations.willow.deployer.statistics.StatisticsSender;
import com.nitorcreations.willow.messages.WebSocketTransmitter;
import com.nitorcreations.willow.messages.event.DeployerStartEvent;
import com.nitorcreations.willow.messages.event.DeployerStopEvent;
import com.nitorcreations.willow.utils.MergeableProperties;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Named
@Singleton
@SuppressFBWarnings(value={"DM_EXIT"}, justification="cli tool needs to convey correct exit code")
@SuppressWarnings("PMD.TooManyStaticImports")
public class Main extends DeployerControl implements MainMBean {
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
      throw new RuntimeException("Failed to register management bean", e);
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
      injector.getInstance(Main.class).doMain(launchUrls.toArray(new String[launchUrls.size()]));
      return;
    }
    registerBean();
    // Statistics
    String statUri = mergedProperties.getProperty(PROPERTY_KEY_STATISTICS_URI);
    if (statUri != null && !statUri.isEmpty()) {
      try {
        long flushInterval = Long.parseLong(mergedProperties.getProperty(PROPERTY_KEY_STATISTICS_FLUSHINTERVAL, "5000"));
        transmitter.setFlushInterval(flushInterval);
        transmitter.setUri(new URI(statUri));
        transmitter.start();
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
              Future<?> job = executor.submit(nextStat);
              if (!job.isDone()) {
                log.fine("Started " + stats.get(i));
              }
            } catch (InstantiationException | IllegalAccessException e) {
              log.info("Failed to start statistic " + stats.get(i) + ":" + e.getMessage());
            }
          }
        }
      } catch (URISyntaxException e) {
        usage(e);
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
    for (final MergeableProperties launchProps : launchPropertiesList) {
      LaunchMethod launcher = null;
      try {
        LaunchMethod.TYPE method = LaunchMethod.TYPE.fromString(launchProps.getProperty(PROPERTY_KEY_PREFIX_LAUNCH + "." + PROPERTY_KEY_SUFFIX_METHOD));
        if (method != null) {
          launcher = injector.getInstance(method.getLauncher());
        } else {
          continue;
        }
      } catch (Throwable t) {
        usage(t);
      }
      File workDir = new File(launchProps.getProperty(PROPERTY_KEY_WORKDIR, "."));
      String propsName = launchProps.getProperty(PROPERTY_KEY_PROPERTIES_FILENAME, "application.properties");
      File propsFile = new File(propsName);
      if (!propsFile.isAbsolute()) {
        propsFile = new File(workDir, propsName);
      }
      File propsDir = propsFile.getParentFile();
      if (!FileUtil.createDir(propsDir)) {
        usage("Unable to create properties directory " + propsDir.getAbsolutePath());
      }
      if (!FileUtil.createDir(workDir)) {
        usage("Unable to create work directory " + workDir.getAbsolutePath());
      }
      try (FileOutputStream out = new FileOutputStream(propsFile)){
        launchProps.store(out, null);
      } catch (IOException e) {
        usage(e);
      }
      try {
        runHooks(PROPERTY_KEY_PREFIX_PRE_START, Collections.singletonList(launchProps), true);
      } catch (Exception e) {
        usage(e);
      }
      if (launcher != null) {
        MergeableProperties childProps = getChildProperties(launchProps, PROPERTY_KEY_PREFIX_LAUNCH, i);
        launcher.setProperties(childProps, new LaunchCallback() {
          @Override
          public void postStop() throws Exception {
            runHooks(PROPERTY_KEY_PREFIX_POST_STOP, Collections.singletonList(launchProps), false);
          }
          @Override
          public void postStart() throws Exception {
            runHooks(PROPERTY_KEY_PREFIX_POST_START, Collections.singletonList(launchProps), false);
          }
          @Override
          public boolean autoRestartDefault() {
            return true;
          }
        });
        Future<Integer> f = executor.submit(launcher);
        if (!f.isDone()) {
          children.add(launcher);
        }
      }
      i++;
    }
    if (statistics.isEmpty() && children.isEmpty()) {
      System.exit(0);
    }
    transmitter.queue(new DeployerStartEvent(sigar.getPid(), deployerName));
  }

  public void stop() {
    for (LaunchMethod next : children) {
      next.stopRelaunching();
    }
    try {
      runHooks(PROPERTY_KEY_PREFIX_SHUTDOWN, launchPropertiesList, false);
    } catch (Exception e) {
      log.log(Level.SEVERE, "Shutdown failed", e);
    }
    if (children.size() > 0) {
      final ExecutorService stopexec = Executors.newFixedThreadPool(children.size() + statistics.size());
      List<Future<Integer>> waits = new ArrayList<>();
      for (final LaunchMethod next : children) {
        waits.add(stopexec.submit(new Callable<Integer>() {
          @Override
          public Integer call() throws Exception {
            return next.destroyChild();
          }
        }));
      }
      for (final StatisticsSender next : statistics) {
        waits.add(stopexec.submit(new Callable<Integer>() {
          @Override
          public Integer call() throws Exception {
            next.stop();
            return 0;
          }
        }));
      }
      int i = 0;
      for (Future<Integer> next : waits) {
        try {
          log.info("Child " + i++ + " returned " + next.get());
        } catch (InterruptedException | ExecutionException e) {
          log.warning("Destroy failed: " + e.getMessage());
        }
      }
      stopexec.shutdownNow();
      transmitter.queue(new DeployerStopEvent(sigar.getPid(), deployerName));
      transmitter.stop();
    }
    super.stop();
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
    log.log(Level.INFO, "Starting deployer faied", e);
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
          log.log(Level.INFO, "Failed to restart child " + next.getName(), e);
        }
      }
    }
  }
  protected void download() {
    List<Future<Integer>> downloads = new ArrayList<>();
    for (MergeableProperties launchProps : launchPropertiesList) {
      if (launchProps.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_URL + "[0]") != null) {
        PreLaunchDownloadAndExtract downloader = new PreLaunchDownloadAndExtract(launchProps);
        downloads.add(executor.submit(downloader));
      }
    }
    int i = 1;
    boolean failures = false;
    for (Future<Integer> next : downloads) {
      try {
        int nextSuccess = next.get();
        if (nextSuccess > -1) {
          log.info("Download " + i++ + " got " + nextSuccess + " items");
        } else {
          log.info("Download " + i++ + " failed (" + -nextSuccess + " attempted)");
          failures = true;
        }
      } catch (InterruptedException | ExecutionException e) {
        log.warning("Download failed: " + e.getMessage());
      }
    }
    if (failures) {
      throw new RuntimeException("Some downloads failed - check logs");
    }
  }
  protected void runHooks(String hookPrefix, List<MergeableProperties> propertiesList, boolean failFast) throws Exception {
    Exception lastThrown = null;
    for (MergeableProperties properties : propertiesList) {
      int i = 0;
      for (String nextMethod : properties.getArrayProperty(hookPrefix, "." + PROPERTY_KEY_SUFFIX_METHOD)) {
        LaunchMethod launcher = null;
        launcher = injector.getInstance(LaunchMethod.TYPE.valueOf(nextMethod.toUpperCase(Locale.ENGLISH)).getLauncher());
        String prefix = hookPrefix + "[" + i + "]";
        MergeableProperties childProps = getChildProperties(properties, prefix, i);
        launcher.setProperties(childProps);
        long timeout = Long.parseLong(properties.getProperty(prefix + PROPERTY_KEY_SUFFIX_TIMEOUT, "30"));
        Future<Integer> ret = executor.submit(launcher);
        try {
          int retVal = ret.get(timeout, TimeUnit.SECONDS);
          log.info(hookPrefix + " returned " + retVal);
          if (retVal != 0 && failFast) {
            throw new Exception("hook " + hookPrefix + "." + i + " failed");
          }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
          log.info(hookPrefix + " failed: " + e.getMessage());
          if (failFast) {
            throw e;
          } else {
            lastThrown = e;
          }
        }
        launcher.destroyChild();
        i++;
      }
    }
    if (lastThrown != null)
      throw lastThrown;
  }
  public static MergeableProperties getChildProperties(MergeableProperties launchProps, String prefix, int index) {
    MergeableProperties childProps = launchProps.getPrefixed(prefix);
    childProps.setProperty(PROPERTY_KEY_DEPLOYER_LAUNCH_INDEX, Integer.toString(index));
    for (String next : new String[] {
      PROPERTY_KEY_DEPLOYER_NAME,
      PROPERTY_KEY_WORKDIR,
      PROPERTY_KEY_DOWNLOAD_DIRECTORY,
      PROPERTY_KEY_DOWNLOAD_RETRIES,
      PROPERTY_KEY_REMOTE_REPOSITORY }) {
      if (launchProps.get(next) != null) {
        childProps.setProperty(next, launchProps.getProperty(next));
      }
    }
    String extraEnvKeys = launchProps.getProperty(prefix + "." + PROPERTY_KEY_SUFFIX_EXTRA_ENV_KEYS);
    if (extraEnvKeys != null) {
      for (String nextKey : extraEnvKeys.split(",")) {
        String key = nextKey.trim();
        childProps.put(key, launchProps.getProperty(key,  ""));
      }
    }

    return childProps;
  }

  public long getFirstJavaChildPid(String childName) {
    return getFirstJavaChildPid(getChildPid(childName));
  }

  public long getFirstJavaChildPid(long nextChild) {
    if (nextChild > 0) {
      try {
        String name = sigar.getProcExe(nextChild).getName();
        if (name.endsWith(".exe")) {
          name = name.substring(0, name.length() - 4);
        }
        if (name.endsWith("w")) {
          name = name.substring(0, name.length() - 1);
        }
        if (name.endsWith("java")) {
          return nextChild;
        } else {
          ProcessQuery q = ProcessQueryFactory.getInstance().getQuery("State.Ppid.eq=" + nextChild);
          for (long next : q.find(new Sigar())) {
            long nextAtt = getFirstJavaChildPid(next);
            if (nextAtt > 0) {
              return nextAtt;
            }
          }
        }
      } catch (SigarException e) {
        log.log(Level.FINE, "Failed to get java child", e);
        return -1;
      }
    }
    return -1;
  }
}
