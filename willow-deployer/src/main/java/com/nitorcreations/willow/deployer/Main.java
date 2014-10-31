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
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_STATISTICS_URI;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_TIMEOUT;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_WORKDIR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;

import com.nitorcreations.willow.messages.WebSocketTransmitter;
import com.nitorcreations.willow.utils.MergeableProperties;
import com.nitorcreations.willow.utils.SimpleFormatter;

public class Main extends DeployerControl implements MainMBean {
	private List<PlatformStatsSender> stats = new ArrayList<>();
	private List<LaunchMethod> children = new ArrayList<>();

    public Main() {
    }
    private void registerBean() {
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
		if (args.length < 2) usage("At least two arguments expected: {name} {launch.properties}");
		setupLogging();
		populateProperties(args);
		MergeableProperties mergedProperties = new MergeableProperties();
		for (int i=launchPropertiesList.size()-1; i>=0; i--) {
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
		WebSocketTransmitter transmitter = null;
		String statUri = mergedProperties.getProperty(PROPERTY_KEY_STATISTICS_URI);
		if (statUri != null && !statUri.isEmpty()) {
			try {
				long flushInterval = Long.parseLong(mergedProperties.getProperty(PROPERTY_KEY_STATISTICS_FLUSHINTERVAL, "5000"));
				transmitter = WebSocketTransmitter.getSingleton(flushInterval, statUri);
				transmitter.start();
			} catch (URISyntaxException e) {
				usage(e);
			}
		}
		if (transmitter != null) {
			try {
				PlatformStatsSender statsSender = new PlatformStatsSender(transmitter, new StatisticsConfig());
				Thread statThread = new Thread(statsSender, "PlatformStatistics");
				statThread.start();
				stats.add(statsSender);
			} catch (Exception e) {
				usage(e);
			}
		}
		//Download
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
		//Stop
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
		//Start
		int i=0;
		for (MergeableProperties launchProps : launchPropertiesList) {
			LaunchMethod launcher = null;
			try {
				launcher = LaunchMethod.TYPE.valueOf(launchProps.getProperty(PROPERTY_KEY_PREFIX_LAUNCH + PROPERTY_KEY_METHOD)).getLauncher();
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
			if (!(propsDir.exists() || propsDir.mkdirs())) {
				usage("Unable to create properties directory " + workDir.getAbsolutePath());
			}
			if (!(workDir.exists() || workDir.mkdirs())) {
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
			launcher.setProperties(launchProps);
			executor.submit(launcher);
			children.add(launcher);
			long pid = launcher.getProcessId();
			if (transmitter != null) {
				try {
					ProcessStatSender statsSender = new ProcessStatSender(transmitter, getMBeanServerConnection(launcher.getProcessId()), pid, new StatisticsConfig());
					Thread statThread = new Thread(statsSender, "ProcessStatistics");
					statThread.start();
					stats.add(statsSender);
				} catch (Exception e) {
					usage(e);
				}
			}
		}
		i++;
	}
	public static void runHooks(String hookPrefix, List<MergeableProperties> propertiesList, boolean failFast) throws Exception {
		ExecutorService exec = Executors.newFixedThreadPool(1);
		Exception lastThrown = null;
		for (MergeableProperties properties : propertiesList) {
			int i=0;
			for (String nextMethod : properties.getArrayProperty(hookPrefix, PROPERTY_KEY_METHOD)) {
				LaunchMethod launcher = null;
				launcher = LaunchMethod.TYPE.valueOf(nextMethod).getLauncher();
				String prefix = hookPrefix + "[" + i + "]";
				launcher.setProperties(properties, prefix);
				long timeout = Long.valueOf(properties.getProperty(prefix + PROPERTY_KEY_TIMEOUT, "30"));
				Future<Integer> ret = exec.submit(launcher);
				try {
					int retVal = ret.get(timeout, TimeUnit.SECONDS);
					log.info(hookPrefix + " returned " + retVal);
					if (retVal != 0 && failFast) {
						throw new Exception("hook " + hookPrefix + "." + i + " failed");
					}
				} catch (InterruptedException | ExecutionException
						| TimeoutException e) {
					log.info(hookPrefix + " failed: " + e.getMessage());
					if (failFast) {
						throw e;
					} else {
						lastThrown = e;
					}
				}
				i++;
			}
		}
		if (lastThrown != null) throw lastThrown;
	}
	public void setupLogging() {
		Logger rootLogger = Logger.getLogger("");
        for (Handler nextHandler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(nextHandler);
        }
        Handler console = new ConsoleHandler();
        console.setLevel(Level.INFO);
        console.setFormatter(new SimpleFormatter());
        rootLogger.addHandler(console);
        rootLogger.setLevel(Level.INFO);
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
			int i=0;
			for (Future<Integer> next : waits) {
				try {
					log.info("Child " + i + " returned " + next.get());
				} catch (InterruptedException | ExecutionException e) {
					log.warning("Destroy failed: " + e.getMessage());
				}
			}
		}
		for (PlatformStatsSender next : stats) {
			next.stop();
		}
	}

	@Override
	public String getStatus() {
		StringBuilder ret = new StringBuilder(deployerName).append(" running ");
		if (children.size() == 1) {
			ret.append("1 child (" + children.get(0).getName() + ": ")
			.append(children.get(0).getProcessId())
			.append(" - restarts: ").append(children.get(0).restarts())
			.append(")");
		} else {
			ret.append(children.size()).append(" children ");
			for (LaunchMethod next : children) {
				ret.append("(" + next.getName() + ": ").append(next.getProcessId())
				.append(" - restarts:").append(next.restarts()).append(") ");
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

}
