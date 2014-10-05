package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.*;

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
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.nitorcreations.willow.messages.WebSocketTransmitter;

public class Main {
    private static final String LOCAL_CONNECTOR_ADDRESS_PROP = "com.sun.management.jmxremote.localConnectorAddress";
    private Logger log = Logger.getLogger(this.getClass().getCanonicalName());
    private List<PlatformStatsSender> stats = new ArrayList<>();
	
	public Main() {	}
	
	public static void main(String[] args) throws URISyntaxException {
		new Main().doMain(args);
	}
	public void doMain(String[] args) {
		if (args.length < 1) usage("At least one argument expected"); 
		Properties launchProperties = getURLProperties(args[0]);
		String deployerName = launchProperties.getProperty(PROPERTY_KEY_DEPLOYER_NAME, args[0]);
		Environment.libc.setenv(ENV_DEPLOYER_NAME, deployerName, 1);
		extractNativeLib(launchProperties);
		WebSocketTransmitter transmitter = null;
		String statUri = launchProperties.getProperty(PROPERTY_KEY_STATISTICS_URI);
		if (statUri != null && !statUri.isEmpty()) {
			try {
				long flushInterval = Long.parseLong(launchProperties.getProperty(PROPERTY_KEY_STATISTICS_FLUSHINTERVAL, "5000"));
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
		int i=0;
		while (i < args.length) {
			LaunchMethod launcher = null;
			try {
				launcher = LaunchMethod.TYPE.valueOf(launchProperties.getProperty(PROPERTY_KEY_LAUNCH_METHOD)).getLauncher();
			} catch (Throwable t) {
				usage(t.getMessage());
			}
			File workDir = new File(launchProperties.getProperty(PROPERTY_KEY_WORKDIR, "."));
			String propsName = launchProperties.getProperty(PROPERTY_KEY_PROPERTIES_FILENAME, "application.properties");
			File propsFile = new File(propsName);
			if (!propsFile.isAbsolute()) {
				propsFile = new File(workDir, propsName);
			}
			if (!(workDir.exists() || workDir.mkdirs())) {
				usage("Unable to create work directory " + workDir.getAbsolutePath());
			}
			try {
				launchProperties.store(new FileOutputStream(propsFile), null);
			} catch (IOException e) {
				usage(e.getMessage());
			}
			PreLaunchDownloadAndExtract downloader = new PreLaunchDownloadAndExtract();
			downloader.execute(launchProperties);
			launcher.setProperties(launchProperties);
			Thread executable = new Thread(launcher);
			executable.start();
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
		if (i < args.length) {
			launchProperties = getURLProperties(args[i]);
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
