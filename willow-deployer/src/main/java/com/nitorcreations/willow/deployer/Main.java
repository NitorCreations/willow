package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.JMX;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.hyperic.sigar.ProcTime;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.ptql.ProcessQuery;
import org.hyperic.sigar.ptql.ProcessQueryFactory;

import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.nitorcreations.core.utils.KillProcess;
import com.nitorcreations.willow.messages.WebSocketTransmitter;
import com.nitorcreations.willow.utils.HostUtil;

public class Main implements MainMBean {
	private static final String LOCAL_CONNECTOR_ADDRESS_PROP = "com.sun.management.jmxremote.localConnectorAddress";
	private Logger log = Logger.getLogger(this.getClass().getCanonicalName());
	private List<PlatformStatsSender> stats = new ArrayList<>();
	private List<Properties> launchPropertiesList = new ArrayList<>();
	private List<LaunchMethod> children = new ArrayList<>();
    public static ObjectName OBJECT_NAME;
	private ExecutorService executor;

    static {
        try {
            OBJECT_NAME = new ObjectName("com.nitorcreations.willow.deployer:type=Main");
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
            assert false;
        }
    }

    public Main() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
			mbs.registerMBean(this, OBJECT_NAME);
		} catch (InstanceAlreadyExistsException | MBeanRegistrationException
				| NotCompliantMBeanException e) {
			e.printStackTrace();
		}
    }

	public static void main(String[] args) throws URISyntaxException {
		new Main().doMain(args);
	}
	public void doMain(String[] args) {
		executor = Executors.newFixedThreadPool(args.length);
		if (args.length < 2) usage("At least two arguments expected: {name} {launch.properties}"); 
		String deployerName = args[0];
		Properties firstProperties = getURLProperties(args[1]);
		firstProperties.setProperty(PROPERTY_KEY_DEPLOYER_NAME, deployerName);
		extractNativeLib(firstProperties);
		WebSocketTransmitter transmitter = null;
		String statUri = firstProperties.getProperty(PROPERTY_KEY_STATISTICS_URI);
		if (statUri != null && !statUri.isEmpty()) {
			try {
				long flushInterval = Long.parseLong(firstProperties.getProperty(PROPERTY_KEY_STATISTICS_FLUSHINTERVAL, "5000"));
				transmitter = WebSocketTransmitter.getSingleton(flushInterval, statUri);
				transmitter.start();
			} catch (URISyntaxException e) {
				usage(e.getMessage());
			}
		}
		if (transmitter != null) {
			try {
				PlatformStatsSender statsSender = new PlatformStatsSender(transmitter, new StatisticsConfig());
				Thread statThread = new Thread(statsSender);
				statThread.start();
				stats.add(statsSender);
			} catch (Exception e) {
				usage(e.getMessage());
			}
		}
		launchPropertiesList.add(firstProperties);
		for (int i=2; i<args.length;i++) {
			launchPropertiesList.add(getURLProperties(args[i]));
		}
		//Download
		List<Future<Integer>> downloads = new ArrayList<>();
		for (Properties launchProps : launchPropertiesList) {
			PreLaunchDownloadAndExtract downloader = new PreLaunchDownloadAndExtract(launchProps);
			downloads.add(executor.submit(downloader));
		}
		int i=0;
		for (Future<Integer> next : downloads) {
			try {
				log.info("Download " + i + " got " + next.get() + " items");
			} catch (InterruptedException | ExecutionException e) {
				log.warning("Download faield: " + e.getMessage());
			}
		}
		//Stop
		LinkedHashMap<Future<Integer>, Long> stopTasks = new LinkedHashMap<>();
		i=0;
		for (Properties launchProps : launchPropertiesList) {
			LaunchMethod stopper = null;
			String stopMethod = launchProps.getProperty(PROPERTY_KEY_PREFIX_SHUTDOWN + PROPERTY_KEY_METHOD);
			if (stopMethod != null) {
				try {
					stopper = LaunchMethod.TYPE.valueOf(stopMethod).getLauncher();
					stopper.setProperties(launchProps, PROPERTY_KEY_PREFIX_SHUTDOWN);
					long timeout = Long.valueOf(launchProps.getProperty(PROPERTY_KEY_PREFIX_POST_STOP + PROPERTY_KEY_TIMEOUT, "5"));
					stopTasks.put(executor.submit(stopper), Long.valueOf(timeout));
				} catch (Throwable t) {
					usage(t.getMessage());
				}
			}
			i++;
		}
		i=0;
		for (Entry<Future<Integer>, Long> next : stopTasks.entrySet()) {
			try {
				log.info("Stopper " + i + " returned "  + next.getKey().get(next.getValue().longValue(), TimeUnit.SECONDS));
			} catch (InterruptedException | ExecutionException
					| TimeoutException e) {
				log.warning("Stopper " + i + " failed "  + e.getMessage());
			}
			i++;
		}
		try {
			Sigar sigar = new Sigar();
			ProcessQuery q = ProcessQueryFactory.getInstance().getQuery("Env." + ENV_DEPLOYER_NAME + ".eq=" + deployerName);
			long minStart = Long.MAX_VALUE;
			long firstPid = 0;
			long mypid = 0;
			long[] pids = q.find(sigar);
			if (pids.length > 1) {
				for (long pid : pids) {
					ProcTime time = sigar.getProcTime(pid);
					if (time.getStartTime() < minStart) {
						minStart = time.getStartTime();
						mypid = firstPid;
						firstPid = pid;
					} else {
						mypid = pid;
					}
				}
				MBeanServerConnection server = getMBeanServerConnection(firstPid);
				MainMBean proxy = JMX.newMBeanProxy(server, OBJECT_NAME, MainMBean.class); 
				try {
					proxy.stop();
				} catch (Throwable e) {
					log.info("JMX stop failed - terminating");
				}
				q = ProcessQueryFactory.getInstance().getQuery("Env." + ENV_DEPLOYER_NAME + ".sw=" + deployerName);
				long termTimeout = Long.parseLong(firstProperties.getProperty(PROPERTY_KEY_DEPLOYER_TERM_TIMEOUT, "60000"));
				long start = System.currentTimeMillis();
				pids = q.find(sigar);
				while (pids.length > 1) {
					for (long nextpid : pids) {
						if (nextpid != mypid) {
							KillProcess.termProcess(Long.toString(nextpid));
						}
					}
					Thread.sleep(500);
					pids = q.find(sigar);
					if (System.currentTimeMillis() > (start + termTimeout)) break;
				}
				for (long nextpid : q.find(sigar)) {
					if (nextpid != mypid) {
						KillProcess.killProcess(Long.toString(nextpid));
					}
				}
			}
		} catch (Throwable e) {
			LogRecord rec = new LogRecord(Level.WARNING, "Failed to kill old deployer");
			rec.setThrown(e);
			log.log(rec);
		}
		//Start
		i=0;
		for (Properties launchProps : launchPropertiesList) {
			LaunchMethod launcher = null;
			try {
				launcher = LaunchMethod.TYPE.valueOf(launchProps.getProperty(PROPERTY_KEY_PREFIX_LAUNCH + PROPERTY_KEY_METHOD)).getLauncher();
			} catch (Throwable t) {
				usage(t.getMessage());
			}
			launchProps.setProperty(PROPERTY_KEY_DEPLOYER_LAUNCH_INDEX, Integer.toString(i));
			File workDir = new File(launchProps.getProperty(PROPERTY_KEY_WORKDIR, "."));
			String propsName = launchProps.getProperty(PROPERTY_KEY_PROPERTIES_FILENAME, "application.properties");
			File propsFile = new File(propsName);
			if (!propsFile.isAbsolute()) {
				propsFile = new File(workDir, propsName);
			}
			if (!(workDir.exists() || workDir.mkdirs())) {
				usage("Unable to create work directory " + workDir.getAbsolutePath());
			}
			try {
				launchProps.store(new FileOutputStream(propsFile), null);
			} catch (IOException e) {
				usage(e.getMessage());
			}
			launcher.setProperties(launchProps);
			executor.submit(launcher);
			children.add(launcher);
			long pid = launcher.getProcessId();
			if (transmitter != null) {
				try {
					ProcessStatSender statsSender = new ProcessStatSender(transmitter, getMBeanServerConnection(launcher.getProcessId()), pid, new StatisticsConfig());
					Thread statThread = new Thread(statsSender);
					statThread.start();
					stats.add(statsSender);
				} catch (Exception e) {
					usage(e.getMessage());
				}
			}
		}
		i++;
	}
	public void stop() {
		for (LaunchMethod next : children) {
			next.stop();
		}
		for (LaunchMethod next : children) {
			try {
				next.waitForChild();
			} catch (InterruptedException e) {
			}
		}
		for (PlatformStatsSender next : stats) {
			next.stop();
		}
	}
	private static void extractNativeLib(Properties launchProperties) {
		String localRepo = launchProperties.getProperty("deployer.local.repository", System.getProperty("user.home") + File.separator + ".deployer" + File.separator + "repository");
		File libDir = new File(new File(localRepo), "lib");
		System.setProperty("java.library.path", libDir.getAbsolutePath());
		String arch = System.getProperty("os.arch");
		String os = System.getProperty("os.name").toLowerCase();
		//libsigar-amd64-linux-1.6.4.so
		String libInJarName = "libsigar-" + arch + "-" + os + "-1.6.4.so";
		String libName = "libsigar-" + arch + "-" + os + ".so";
		File libFile = new File(libDir, libName);
		if (!(libFile.exists() && libFile.canExecute())) {
			InputStream lib = Main.class.getClassLoader().getResourceAsStream(libInJarName);
			libDir.mkdirs();
			if (lib != null) {
				try (OutputStream out = new FileOutputStream(libFile)) {
					byte[] buffer = new byte[1024];
					int len;
					while ((len = lib.read(buffer)) != -1) {
						out.write(buffer, 0, len);
					}
					libFile.setExecutable(true, false);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						lib.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				throw new RuntimeException("Failed to find " + libName);
			}
		}
	}

	public MBeanServerConnection getMBeanServerConnection(long lvmid) throws Exception {
		String host = null;
		MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost(host);

		log.fine("Inspecting VM " + lvmid);
		MonitoredVm vm = null;
		String vmidString = "//" + lvmid + "?mode=r";
		try {
			VmIdentifier id = new VmIdentifier(vmidString);
			vm = monitoredHost.getMonitoredVm(id, 0);
		} catch (URISyntaxException e) {
			// Should be able to generate valid URLs
			assert false;
		} catch (Exception e) {
		} finally {
			if (vm == null) {
				return null;
			}
		}

		log.finer("VM " + lvmid + " is a our vm");
		Monitor command = vm.findByName("sun.rt.javaCommand");
		String lcCommandStr = command.getValue().toString()
				.toLowerCase();

		log.finer("Command for beanserver VM " + lvmid + ": " + lcCommandStr);

		try {
			VirtualMachine attachedVm = VirtualMachine
					.attach("" + lvmid);
			String home = attachedVm.getSystemProperties()
					.getProperty("java.home");

			// Normally in ${java.home}/jre/lib/management-agent.jar but might
			// be in ${java.home}/lib in build environments.
			File f = Paths.get(home, "jre", "lib", "management-agent.jar").toFile();
			if (!f.exists()) {
				f = Paths.get(home,  "lib", "management-agent.jar").toFile();
				if (!f.exists()) {
					throw new IOException("Management agent not found");
				}
			}

			String agent = f.getCanonicalPath();
			log.finer("Found agent for VM " + lvmid + ": " + agent);
			try {
				attachedVm.loadAgent(agent,	"com.sun.management.jmxremote");
			} catch (AgentLoadException x) {
				return null;
			} catch (AgentInitializationException x) {
				return null;
			}
			Properties agentProps = attachedVm.getAgentProperties();
			String address = (String) agentProps.get(LOCAL_CONNECTOR_ADDRESS_PROP);
			JMXServiceURL url = new JMXServiceURL(address);
			JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
			MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
			vm.detach();                            
			return mbsc;
		} catch (AttachNotSupportedException x) {
			log.log(Level.WARNING,
					"Not attachable", x);
		} catch (IOException x) {
			log.log(Level.WARNING,
					"Failed to get JMX connection", x);
		}

		return null;
	}

	private static void usage(String error) {
		System.err.println(error);
		System.err.println(Main.class.getName() + " [property-url]");
		System.exit(1);
	}

	private static Proxy resolveProxy(String proto) throws MalformedURLException {
		String proxyUrl = System.getenv(proto.toLowerCase() + "_proxy");
		if (proxyUrl == null) {
			proxyUrl = System.getenv(proto.toUpperCase() + "_PROXY");
		}
		if (proxyUrl == null) return null;
		URL proxyAddr = new URL(proxyUrl);
		return new Proxy(Type.HTTP, new InetSocketAddress(proxyAddr.getHost(), proxyAddr.getPort()));
	}
	private Properties getURLProperties(String url) {
		Properties launchProperties = new Properties();
		try {
			URL propertyURL = new URL(url);
			Proxy p = resolveProxy(propertyURL.getProtocol());
			URLConnection conn; 
			if (p == null) {
				conn = propertyURL.openConnection();
			} else {
				conn = propertyURL.openConnection(p);
			}
			conn.setDoInput(true);
			conn.connect();
			launchProperties.load(conn.getInputStream());
		} catch (IOException e) {
			usage(e.getMessage());
		}
		return launchProperties;
	}
}
