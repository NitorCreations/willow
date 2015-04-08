package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.ENV_DEPLOYER_NAME;
import static com.nitorcreations.willow.deployer.PropertyKeys.ENV_DEPLOYER_TERM_TIMEOUT;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DEPLOYER_HOST;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DEPLOYER_NAME;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.ptql.ProcessQuery;
import org.hyperic.sigar.ptql.ProcessQueryFactory;

import sun.management.ConnectorAddressLink;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.nitorcreations.core.utils.KillProcess;
import com.nitorcreations.willow.protocols.Register;
import com.nitorcreations.willow.utils.MergeableProperties;
import com.nitorcreations.willow.utils.SimpleFormatter;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

@SuppressWarnings("restriction")
public class DeployerControl {
  protected final static Logger log = Logger.getLogger("deployer");
  @Inject
  protected ExecutorService executor;
  @Inject
  protected SigarProxy sigar;
  protected final List<MergeableProperties> launchPropertiesList = new ArrayList<>();
  protected String deployerName;
  protected static Injector injector;
  public static ObjectName OBJECT_NAME;
  static {
    try {
      OBJECT_NAME = new ObjectName("com.nitorcreations.willow.deployer:type=Main");
      setupLogging();
      Register.doIt();
      injector = Guice.createInjector(
        new WireModule(new DeployerModule(), new SpaceModule(
            new URLClassSpace(DeployerControl.class.getClassLoader())
            )));
      System.setProperty(PROPERTY_KEY_DEPLOYER_HOST, 
        injector.getInstance(SigarProxy.class).getNetInfo().getHostName());
    } catch (Throwable e) {
      e.printStackTrace();
      assert false;
    }
  }

  public void stopOld(String[] args) {
    if (args.length < 1)
      usage("At least one argument expected: {name}");
    deployerName = args[0];
    // Stop
    try {
      long mypid = sigar.getPid();
      if (mypid <= 0)
        throw new RuntimeException("Failed to resolve own pid");
      long firstPid = findOldDeployerPid(deployerName);
      String timeOutEnv = System.getenv(ENV_DEPLOYER_TERM_TIMEOUT);
      long termTimeout = 60000;
      if (timeOutEnv != null) {
        termTimeout = Long.valueOf(timeOutEnv);
      }
      if (firstPid > 0) {
        try (JMXConnector conn = getJMXConnector(firstPid)) {
          MBeanServerConnection server = conn.getMBeanServerConnection();
          MainMBean proxy = JMX.newMBeanProxy(server, OBJECT_NAME, MainMBean.class);
          proxy.stop();
        } catch (Throwable e) {
          log.info("JMX stop failed - terminating");
          KillProcess.killProcess(Long.toString(firstPid));
        }
        // Processes with old deployer as parent
        killWithQuery("State.Ppid.eq=" + firstPid, termTimeout, mypid);
      }
      // Old deployer identified by deployerName in environment
      //killWithQuery("Env." + ENV_DEPLOYER_NAME + ".eq=" + deployerName, termTimeout, mypid);
    } catch (Throwable e) {
      LogRecord rec = new LogRecord(Level.WARNING, "Failed to kill old deployer");
      rec.setThrown(e);
      log.log(rec);
    }
  }
  protected Set<Long> getPidsExcludingMyPid(String query, long mypid) throws SigarException {
    ProcessQuery q = ProcessQueryFactory.getInstance().getQuery(query);
    long[] pids = q.find(new Sigar());
    Set<Long> pidSet = new HashSet<>();
    for (long next : pids) {
      if (next != mypid)
        pidSet.add(next);
    }
    return pidSet;
  }    
  protected void killWithQuery(String query, long termTimeout, long mypid) throws SigarException, IOException, InterruptedException {
    long start = System.currentTimeMillis();
    Set<Long> pids = getPidsExcludingMyPid(query, mypid);
    while (pids.size() > 0) {
      for (Long nextpid : pids) {
        KillProcess.termProcess(nextpid.toString());
      }
      Thread.sleep(500);
      pids = getPidsExcludingMyPid(query, mypid);
      if (System.currentTimeMillis() > (start + termTimeout))
        break;
    }
    while (pids.size() > 0) {
      for (Long nextpid : pids) {
        KillProcess.killProcess(nextpid.toString());
      }
      Thread.sleep(500);
      pids = getPidsExcludingMyPid(query, mypid);
    }
  }
  public static JMXConnector getJMXConnector(long pid) {
    try {
      String address = ConnectorAddressLink.importFrom((int) pid);
      if (address == null) {
          startManagementAgent(pid);
          address = ConnectorAddressLink.importFrom((int) pid);
      }
      JMXServiceURL jmxUrl = new JMXServiceURL(address);
      return JMXConnectorFactory.connect(jmxUrl);
    } catch (IOException | AttachNotSupportedException | AgentLoadException | AgentInitializationException e) {
      log.info("Failed to create JMXConnector to " + pid + ": " + e.getMessage());
      return null;
    }
  }
  private static void startManagementAgent(long pid) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
      VirtualMachine attachedVm = VirtualMachine.attach("" + pid);
      String home = attachedVm.getSystemProperties().getProperty("java.home");
      File f = Paths.get(home, "jre", "lib", "management-agent.jar").toFile();
      if (!f.exists()) {
        f = Paths.get(home, "lib", "management-agent.jar").toFile();
        if (!f.exists()) {
          throw new IOException("Management agent not found");
        }
      }
      String agent = f.getCanonicalPath();
      log.fine("Loading " + agent + " into target VM...");
      attachedVm.loadAgent(agent);
      attachedVm.detach();
  }
  protected void usage(String error) {
    System.err.println(error);
    System.exit(1);
  }
  private static Proxy resolveProxy(String proto, String hostName) throws MalformedURLException {
    String proxyUrl = System.getenv(proto.toLowerCase() + "_proxy");
    if (proxyUrl == null) {
      proxyUrl = System.getenv(proto.toUpperCase() + "_PROXY");
    }
    if (proxyUrl == null)
      return null;
    String noProxy = System.getenv("no_proxy");
    if (noProxy == null) {
      noProxy = System.getenv("NO_PROXY");
    }
    if (!noProxyMatches(hostName, noProxy)) {
      URL proxyAddr = new URL(proxyUrl);
      return new Proxy(Type.HTTP, new InetSocketAddress(proxyAddr.getHost(), proxyAddr.getPort()));
    } else {
      return null;
    }
  }
  protected static boolean noProxyMatches(String host, String noProxy) {
    if (noProxy == null)
      return false;
    for (String next : noProxy.split(",")) {
      String trimmed = next.trim();
      while (trimmed.startsWith(".")) {
        trimmed = trimmed.substring(1);
      }
      if (trimmed.equals(host) || host.endsWith("." + trimmed)) {
        return true;
      }
    }
    return false;
  }
  protected MergeableProperties getURLProperties(String url) {
    MergeableProperties launchProperties = new MergeableProperties();
    launchProperties.merge(System.getProperties(), url);
    return launchProperties;
  }
  protected void populateProperties(String[] args) {
    deployerName = args[0];
    for (int i = 1; i < args.length; i++) {
      MergeableProperties next = getURLProperties(args[i]);
      next.setProperty(PROPERTY_KEY_DEPLOYER_NAME, deployerName);
      launchPropertiesList.add(next);
    }
  }
  protected Set<Long> findChildPids() throws SigarException {
    long mypid = sigar.getPid();
    return getPidsExcludingMyPid("Env." + ENV_DEPLOYER_NAME + ".re=.+,Env.W_DEPLOYER_IDENTIFIER.re=.+", mypid);
  }
  protected Set<Long> findChildPids(String deployerName) throws SigarException {
    long mypid = sigar.getPid();
    return getPidsExcludingMyPid("Env." + ENV_DEPLOYER_NAME + ".eq=" + deployerName +
      ",Env.W_DEPLOYER_IDENTIFIER.re=.+", mypid);
  }
  protected Set<Long> findOldDeployerPids() throws SigarException {
    long mypid = sigar.getPid();
    Set<Long> children = findChildPids();
    Set<Long> pids = getPidsExcludingMyPid("Env." + ENV_DEPLOYER_NAME + ".re=.+", mypid);
    Set<Long> parentPids = new HashSet<>();
    for (Long next : pids) {
      if (!children.contains(next)) {
        String name = sigar.getProcExe(next).getName();
        if (name.endsWith(".exe")) {
          name = name.substring(0, name.length() - 4);
        }
        if (name.endsWith("w")) {
          name = name.substring(0, name.length() - 1);
        }
        if (name.endsWith("java")) parentPids.add(next);
      }
    }
    return parentPids;
  }
  protected long findOldDeployerPid(String deployerName) throws SigarException {
    long mypid = sigar.getPid();
    Set<Long> children = findChildPids(deployerName);
    Set<Long> pids = getPidsExcludingMyPid("Env." + ENV_DEPLOYER_NAME + ".eq=" + deployerName, mypid);
    if (pids.isEmpty()) {
      return -1;
    }
    for (Long next : pids) {
      if (!children.contains(next)) {
        String name = sigar.getProcExe(next).getName();
        if (name.endsWith(".exe")) {
          name = name.substring(0, name.length() - 4);
        }
        if (name.endsWith("w")) {
          name = name.substring(0, name.length() - 1);
        }
        if (name.endsWith("java")) return next;
      }
    }
    return -1;
  }
  public static void setupLogging() {
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
  public void stop() {
    executor.shutdownNow();
  }
}