package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.ENV_DEPLOYER_PARENT_NAME;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_AUTORESTART;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DEPLOYER_LAUNCH_INDEX;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DEPLOYER_NAME;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_EXTRA_ENV_KEYS;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_LAUNCH_WORKDIR;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_LAUNCH;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_POST_START;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_POST_STOP;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_PRE_START;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_STATISTICS_URI;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_TERM_TIMEOUT;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_WORKDIR;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.ptql.ProcessQuery;
import org.hyperic.sigar.ptql.ProcessQueryFactory;

import com.nitorcreations.willow.messages.WebSocketTransmitter;
import com.nitorcreations.willow.utils.AbstractStreamPumper;
import com.nitorcreations.willow.utils.LoggingStreamPumper;
import com.nitorcreations.willow.utils.MergeableProperties;


public abstract class AbstractLauncher implements LaunchMethod {
	protected final String PROCESS_IDENTIFIER = new BigInteger(130, new SecureRandom()).toString(32);
	protected final Set<String> launchArgs = new LinkedHashSet<String>();
	protected MergeableProperties launchProperties;
	protected URI statUri;
	protected WebSocketTransmitter transmitter = null;
	protected Process child;
	protected AtomicInteger returnValue= new AtomicInteger(-1);
	protected Map<String, String> extraEnv = new HashMap<>();
	protected File workingDir;
	protected String keyPrefix;
	protected AtomicBoolean running = new AtomicBoolean(true);
	protected AbstractStreamPumper stdout, stderr;
	private String name;
	@Override
	public String getName() {
		return name;
	}
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	private int restarts=0;
	
	public long getProcessId() {
		Sigar sigar = new Sigar();
		long stopTrying = System.currentTimeMillis() + 1000 * 60;
		while (System.currentTimeMillis() < stopTrying) {
			try {
				ProcessQuery q = ProcessQueryFactory.getInstance().getQuery("Env." + ENV_KEY_DEPLOYER_IDENTIFIER + ".eq=" + PROCESS_IDENTIFIER);
				return q.findProcess(sigar);
			} catch (Throwable e) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
				}
			}
		}
		throw new RuntimeException("Failed to resolve pid");
	}

	@Override
	public Integer call() {
		return launch(extraEnv, getLaunchArgs());
	}
	@Override
	public void setProperties(MergeableProperties properties) {
		this.setProperties(properties, PROPERTY_KEY_PREFIX_LAUNCH);
	}
	protected Integer launch(String ... args) {
		return launch(new HashMap<String, String>(), args);
	}
	protected Integer launch(Map<String, String> extraEnv, String ... args) {
		while (running.get() && !Thread.interrupted()) {
			restarts++;
			String autoRestartDefault = "false";
			if (PROPERTY_KEY_PREFIX_LAUNCH.equals(keyPrefix)) {
				autoRestartDefault = "true";
			}
			name = launchProperties.getProperty(keyPrefix, launchProperties.getProperty(PROPERTY_KEY_DEPLOYER_NAME)
					+ "." +  launchProperties.getProperty(PROPERTY_KEY_DEPLOYER_LAUNCH_INDEX, "0"));
			String parentName = launchProperties.getProperty(PROPERTY_KEY_DEPLOYER_NAME);
			boolean autoRestart = Boolean.valueOf(launchProperties.getProperty(keyPrefix + PROPERTY_KEY_AUTORESTART, autoRestartDefault));
			running.set(autoRestart);
			Logger log = Logger.getLogger(name);
			ProcessBuilder pb = new ProcessBuilder(args);
			pb.environment().putAll(System.getenv());
			pb.environment().putAll(extraEnv);
			pb.environment().put(ENV_KEY_DEPLOYER_IDENTIFIER, PROCESS_IDENTIFIER);
			pb.environment().put(ENV_DEPLOYER_PARENT_NAME, parentName);
			pb.directory(workingDir);
			log.info(String.format("Starting %s%n", pb.command().toString()));
			try {
				child = pb.start();
				if (transmitter != null) {
					stdout = new StreamLinePumper(child.getInputStream(), transmitter, "STDOUT");
					stderr = new StreamLinePumper(child.getErrorStream(), transmitter, "STDERR");
				} else {
					stdout = new LoggingStreamPumper(child.getInputStream(), Level.INFO, name);
					stderr = new LoggingStreamPumper(child.getErrorStream(), Level.INFO, name);
				}
				new Thread(stdout, name + "-child-stdout-pumper").start();
				new Thread(stderr, name + "-child-sdrerr-pumper").start();
				try {
					if (PROPERTY_KEY_PREFIX_LAUNCH.equals(keyPrefix)) {
						Main.runHooks(PROPERTY_KEY_PREFIX_POST_START, Collections.singletonList(launchProperties), false);
					}
				} catch (Exception e) {
					LogRecord rec = new LogRecord(Level.WARNING, "Failed to run post start");
					rec.setThrown(e);
					log.log(rec);
				}
				returnValue.set(child.waitFor());
			} catch (IOException e) {
				LogRecord rec = new LogRecord(Level.WARNING, "Failed to start  process");
				rec.setThrown(e);
				log.log(rec);
			} catch (InterruptedException e) {
				LogRecord rec = new LogRecord(Level.WARNING, "Failed to start process stream pumpers");
				rec.setThrown(e);
				log.log(rec);
			} finally {
				try {
					if (PROPERTY_KEY_PREFIX_LAUNCH.equals(keyPrefix)) {
						Main.runHooks(PROPERTY_KEY_PREFIX_POST_STOP, Collections.singletonList(launchProperties), false);
					}
				} catch (Exception e) {
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}
		try {
			return destroyChild();
		} catch (InterruptedException e) {
			return returnValue.get();
		}
	}
	@Override
	public void stopRelaunching() {
		running.set(false);
	}
	@Override
	public int destroyChild() throws InterruptedException {
		if (child != null) {
			child.destroy();
		}
		if (stderr != null) {
			stderr.stop();
		}
		if (stdout != null) {
			stdout.stop();
		}
		executor.shutdown();
		long timeout = Long.valueOf(launchProperties.getProperty(keyPrefix + PROPERTY_KEY_TERM_TIMEOUT, "30"));
		executor.awaitTermination(timeout, TimeUnit.SECONDS);
		executor.shutdownNow();
		if (child != null) {
			child.waitFor();
		}
		return getReturnValue();
	}
	public synchronized int getReturnValue() {
		return returnValue.get();
	}
	protected void addLauncherArgs(MergeableProperties properties, String prefix) {
		launchArgs.addAll(properties.getArrayProperty(prefix));
	}
	protected String[] getLaunchArgs() {
		return launchArgs.toArray(new String[launchArgs.size()]);
	}

	public void setProperties(MergeableProperties properties, String keyPrefix) {
		this.keyPrefix = keyPrefix;
		launchProperties = properties;
		try {
			String statisticsUri = properties.getProperty(PROPERTY_KEY_STATISTICS_URI);
			if (statisticsUri != null) {
				statUri = new URI(statisticsUri);
			}
		} catch (URISyntaxException e) {
		}
		try {
			if (statUri != null) {
				long flushInterval = Long.parseLong(properties.getProperty("statistics.flushinterval", "2000"));
				transmitter = WebSocketTransmitter.getSingleton(flushInterval, statUri.toString());
			}
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to initialize launcher", e);
		}
		String extraEnvKeys = properties.getProperty(keyPrefix + PROPERTY_KEY_EXTRA_ENV_KEYS);
		if (extraEnvKeys != null) {
			for (String nextKey : extraEnvKeys.split(",")) {
				extraEnv.put(nextKey.trim(), properties.getProperty((nextKey.trim())));
			}
		}
		workingDir = new File(properties.getProperty(keyPrefix + PROPERTY_KEY_LAUNCH_WORKDIR,
				properties.getProperty(PROPERTY_KEY_WORKDIR, ".")));
		
	}
	@Override
	public int restarts() {
		return restarts;
	}
}
