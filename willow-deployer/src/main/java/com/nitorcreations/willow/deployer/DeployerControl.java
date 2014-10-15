package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.ENV_DEPLOYER_LOCAL_REPOSITORY;
import static com.nitorcreations.willow.deployer.PropertyKeys.ENV_DEPLOYER_NAME;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DEPLOYER_NAME;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DEPLOYER_TERM_TIMEOUT;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_LAUNCH_URLS;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_METHOD;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_POST_STOP;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_SHUTDOWN;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_TIMEOUT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
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

import com.nitorcreations.core.utils.KillProcess;
import com.nitorcreations.willow.utils.MergeableProperties;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

public class DeployerControl {
	private static final String LOCAL_CONNECTOR_ADDRESS_PROP = "com.sun.management.jmxremote.localConnectorAddress";
	protected final Logger log = Logger.getLogger(this.getClass().getCanonicalName());
	protected final ExecutorService executor = Executors.newFixedThreadPool(10);
	protected final List<MergeableProperties> launchPropertiesList = new ArrayList<>();
	protected String deployerName;
    public static ObjectName OBJECT_NAME;

    static {
        try {
            OBJECT_NAME = new ObjectName("com.nitorcreations.willow.deployer:type=Main");
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
            assert false;
        }
    }

	public void stopOld(String[] args) {
		if (args.length < 2) usage("At least two arguments expected: {name} {launch.properties}"); 
		populateProperties(args);
		MergeableProperties mergedProperties = new MergeableProperties();
		for (int i=launchPropertiesList.size()-1; i>=0; i--) {
			mergedProperties.putAll(launchPropertiesList.get(i));
		}
		List<String> launchUrls = mergedProperties.getArrayProperty(PROPERTY_KEY_LAUNCH_URLS);
		if (launchUrls.size() > 0) {
			launchUrls.add(0, deployerName);
			launchPropertiesList.clear();
			stopOld(launchUrls.toArray(new String[launchUrls.size()]));
			return;
		}
		extractNativeLib();
		//Stop
		LinkedHashMap<Future<Integer>, Long> stopTasks = new LinkedHashMap<>();
		int i=0;
		for (MergeableProperties launchProps : launchPropertiesList) {
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
				long termTimeout = Long.parseLong(mergedProperties.getProperty(PROPERTY_KEY_DEPLOYER_TERM_TIMEOUT, "60000"));
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
	protected static void usage(String error) {
		System.err.println(error);
		System.err.println(Main.class.getName() + " [property-url]");
		System.exit(1);
	}
	protected static void extractNativeLib() {
		String localRepo = System.getenv(ENV_DEPLOYER_LOCAL_REPOSITORY);
		if (localRepo == null) {
          localRepo = System.getProperty("user.home") + File.separator + ".deployer" + File.separator + "repository";
		}
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
	private static Proxy resolveProxy(String proto, String hostName) throws MalformedURLException {
		String proxyUrl = System.getenv(proto.toLowerCase() + "_proxy");
		if (proxyUrl == null) {
			proxyUrl = System.getenv(proto.toUpperCase() + "_PROXY");
		}
		if (proxyUrl == null) return null;
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
		if (noProxy == null) return false;
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
		try {
			URL propertyURL = new URL(url);
			Proxy p = resolveProxy(propertyURL.getProtocol(), propertyURL.getHost());
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
	protected void populateProperties(String[] args) {
		if (args.length != (launchPropertiesList.size() + 1));
		launchPropertiesList.clear();
		deployerName = args[0];
		for (int i=1; i<args.length;i++) {
			MergeableProperties next = getURLProperties(args[i]);
			next.setProperty(PROPERTY_KEY_DEPLOYER_NAME, deployerName);
			launchPropertiesList.add(next);
		}

	}
}
