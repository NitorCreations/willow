package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DEPLOYER_LAUNCH_INDEX;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_METHOD;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_LAUNCH;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PROPERTIES_FILENAME;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_STATISTICS_FLUSHINTERVAL;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_STATISTICS_URI;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_WORKDIR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;

import com.nitorcreations.willow.messages.WebSocketTransmitter;

public class Main extends DeployerControl implements MainMBean {
	private List<PlatformStatsSender> stats = new ArrayList<>();
	private List<LaunchMethod> children = new ArrayList<>();

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
		if (args.length < 2) usage("At least two arguments expected: {name} {launch.properties}"); 
		populateProperties(args);
		WebSocketTransmitter transmitter = null;
		Properties firstProperties = launchPropertiesList.get(0);
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
				log.warning("Download failed: " + e.getMessage());
			}
		}
		//Stop
		stopOld(args);
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
			next.stopRelaunching();
		}
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
		for (PlatformStatsSender next : stats) {
			next.stop();
		}
	}

	@Override
	public String getStatus() {
		StringBuilder ret = new StringBuilder(deployerName).append(" running ");
		if (children.size() == 1) {
			ret.append("1 child (PID:").append(children.get(0).getProcessId())
			.append(" - restarts: ").append(children.get(0).restarts())
			.append(")");
		} else {
			ret.append(children.size()).append(" children ");
			for (LaunchMethod next : children) {
				ret.append("(PID:").append(children.get(0).getProcessId())
				.append(" - restarts:").append(children.get(0).restarts()).append(") ");
			}
			ret.setLength(ret.length() - 1);
		}
		return ret.toString();
	}
}
