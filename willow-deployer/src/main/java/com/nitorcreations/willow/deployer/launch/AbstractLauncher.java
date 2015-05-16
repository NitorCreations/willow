package com.nitorcreations.willow.deployer.launch;

import static com.nitorcreations.willow.deployer.PropertyKeys.ENV_DEPLOYER_IDENTIFIER;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DEPLOYER_LAUNCH_INDEX;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DEPLOYER_NAME;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_AUTORESTART;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRA_ENV_KEYS;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_LAUNCH_WORKDIR;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_SKIPOUTPUTREDIRECT;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_TERM_TIMEOUT;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_WORKDIR;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessQuery;
import org.hyperic.sigar.ptql.ProcessQueryFactory;

import com.nitorcreations.willow.deployer.download.FileUtil;
import com.nitorcreations.willow.messages.WebSocketTransmitter;
import com.nitorcreations.willow.messages.event.ChildDiedEvent;
import com.nitorcreations.willow.messages.event.ChildRestartedEvent;
import com.nitorcreations.willow.messages.event.ChildRestartingEvent;
import com.nitorcreations.willow.messages.event.ChildStartedEvent;
import com.nitorcreations.willow.messages.event.ChildStartingEvent;
import com.nitorcreations.willow.messages.event.ChildStoppedEvent;
import com.nitorcreations.willow.messages.event.ChildStoppingEvent;
import com.nitorcreations.willow.utils.AbstractStreamPumper;
import com.nitorcreations.willow.utils.LoggingStreamPumper;
import com.nitorcreations.willow.utils.MergeableProperties;

public abstract class AbstractLauncher implements LaunchMethod {
  @Inject
  protected WebSocketTransmitter transmitter;
  protected final String PROCESS_IDENTIFIER = new BigInteger(130, new SecureRandom()).toString(32);
  protected final Set<String> launchArgs = new LinkedHashSet<>();
  protected MergeableProperties launchProperties;
  protected URI statUri;
  protected Process child;
  protected AtomicInteger returnValue = new AtomicInteger(-1);
  protected Map<String, String> extraEnv = new HashMap<>();
  protected File workingDir;
  protected AtomicBoolean running = new AtomicBoolean(true);
  protected AtomicBoolean restarting = new AtomicBoolean(false);
  protected AtomicLong pid = new AtomicLong(-1);
  protected AbstractStreamPumper stdout, stderr;
  private String name;
  @Inject
  private ExecutorService executor;
  private int restarts = 0;
  private Logger log;
  private LaunchCallback callback = null;

  @Override
  public String getName() {
    return name;
  }
  public long getProcessId() {
    if (pid.get() > 0) {
      return pid.get();
    }
    long stopTrying = System.currentTimeMillis() + 1000 * 60;
    while (pid.get() < 0 && System.currentTimeMillis() < stopTrying) {
      try {
        ProcessQuery q = ProcessQueryFactory.getInstance().getQuery("Env." + ENV_DEPLOYER_IDENTIFIER + ".eq=" + PROCESS_IDENTIFIER);
        long newPid = q.findProcess(new Sigar());
        if (newPid > 0) {
          pid.set(newPid);
          return pid.get();
        }
      } catch (SigarException e) {
        log.log(Level.FINE, "Failed to get PID", e);
      } catch (Throwable e) {
        e.printStackTrace();
        try {
          Thread.sleep(500);
        } catch (InterruptedException e1) {}
      }
    }
    throw new RuntimeException("Failed to resolve pid");
  }

  @Override
  public Integer call() {
    return launch(extraEnv, getLaunchArgs());
  }

  protected Integer launch(LaunchCallback callback, String... args) {
    return launch(new HashMap<String, String>(), args);
  }

  protected Integer launch(Map<String, String> extraEnv, String... args) {
    while (running.get() && !Thread.interrupted()) {
      restarts++;
      String autoRestartDefaultVal = "false";
      if (callback != null) {
        autoRestartDefaultVal = Boolean.toString(callback.autoRestartDefault());
      }
      boolean autoRestart = Boolean.valueOf(launchProperties.getProperty(PROPERTY_KEY_SUFFIX_AUTORESTART, autoRestartDefaultVal));
      if (autoRestart && !restarting.get()) {
        //launching the child instead of a one-off hook task
        transmitter.queue(new ChildStartingEvent(getName()));
      }
      running.set(autoRestart);
      log = Logger.getLogger(name);
      ProcessBuilder pb = new ProcessBuilder(args);
      LinkedHashMap<String, String> copyEnv = new LinkedHashMap<>(System.getenv());
      pb.environment().putAll(copyEnv);
      pb.environment().putAll(extraEnv);
      pb.environment().put(ENV_DEPLOYER_IDENTIFIER, PROCESS_IDENTIFIER);
      if (!FileUtil.createDir(workingDir)) {
        throw new RuntimeException("Failed to create working directory");
      }
      pb.directory(workingDir);
      log.info(String.format("Starting %s%n", pb.command().toString()));
      try {
        synchronized(this) {
          child = pb.start();
        }
        if (transmitter != null && transmitter.isRunning() &&
          launchProperties.getProperty(PROPERTY_KEY_SUFFIX_SKIPOUTPUTREDIRECT) == null) {
          stdout = new StreamLinePumper(child.getInputStream(), transmitter, "STDOUT");
          stderr = new StreamLinePumper(child.getErrorStream(), transmitter, "STDERR");
        } else {
          stdout = new LoggingStreamPumper(child.getInputStream(), Level.INFO, name);
          stderr = new LoggingStreamPumper(child.getErrorStream(), Level.INFO, name);
        }
        new Thread(stdout, name + "-child-stdout-pumper").start();
        new Thread(stderr, name + "-child-sdrerr-pumper").start();
        try {
          if (callback != null) {
            callback.postStart();
          }
        } catch (Exception e) {
          log.log(Level.WARNING, "Failed to run post start", e);
        }
        if (autoRestart && !restarting.get()) {
          transmitter.queue(new ChildStartedEvent(getName(), getProcessId()));
        } else if (autoRestart) {
          transmitter.queue(new ChildRestartedEvent(getName(), getProcessId()));
          restarting.set(false);
        }

        returnValue.set(child.waitFor());

        if (autoRestart && running.get() && !restarting.get()) {
          transmitter.queue(new ChildDiedEvent(getName(), pid.get(), returnValue.get()));
        } else if (autoRestart) {
          transmitter.queue(new ChildStoppedEvent(getName(), pid.get(), returnValue.get(), restarting.get()));
        }
        pid.set(-1);
      } catch (IOException e) {
        log.log(Level.WARNING, "Failed to start  process", e);
      } catch (InterruptedException e) {
        log.log(Level.WARNING, "Failed to start process stream pumpers", e);
      } finally {
        if (callback != null) {
          try {
            callback.postStop();
          } catch (Exception e) {
            log.log(Level.WARNING, "Failed to run post stop", e);
          }
        }
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {}
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
  public synchronized int destroyChild() throws InterruptedException {
    if (child == null) {
      log.finest("No child to destroy, returning previous return value");
      return getReturnValue();
    };
    long timeout = Long.valueOf(launchProperties.getProperty(PROPERTY_KEY_SUFFIX_TERM_TIMEOUT, "30"));
    if (isChildAlive()) {
      if (running.get()) {
        transmitter.queue(new ChildRestartingEvent(getName()));
        restarting.set(true);
      } else {
        transmitter.queue(new ChildStoppingEvent(getName(), pid.get()));
      }
      child.destroy();

      try {
        Future<Boolean> waitFor = executor.submit(new Callable<Boolean>() {
          public Boolean call() throws InterruptedException {
            child.waitFor();
            pid.set(-1);
            return true;
          }
        });
        waitFor.get(timeout, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        if (log != null) {
          log.info("Child did not stop on TERM");
        }
      } finally {
        if (pid.get() > 0) {
          try {
            new Sigar().kill(pid.get(), 9);
            pid.set(-1);
          } catch (SigarException e) {
            if (log != null) {
              log.log(Level.INFO, "Failed to kill child " + getName(), e);
            }
          }
        }
      }
    }
    if (stderr != null) {
      stderr.stop();
    }
    if (stdout != null) {
      stdout.stop();
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
  @Override
  public void setProperties(MergeableProperties properties) {
    setProperties(properties, null);
  }
  @Override
  public void setProperties(MergeableProperties properties, LaunchCallback callback) {
    this.callback = callback;
    launchProperties = properties;
    String extraEnvKeys = properties.getProperty(PROPERTY_KEY_SUFFIX_EXTRA_ENV_KEYS);
    if (extraEnvKeys != null) {
      for (String nextKey : extraEnvKeys.split(",")) {
        extraEnv.put(nextKey.trim(), properties.getProperty((nextKey.trim())));
      }
    }
    name = launchProperties.getProperty("", launchProperties.getProperty(PROPERTY_KEY_DEPLOYER_NAME) + "." + launchProperties.getProperty(PROPERTY_KEY_DEPLOYER_LAUNCH_INDEX, "0"));
    workingDir = new File(properties.getProperty(PROPERTY_KEY_SUFFIX_LAUNCH_WORKDIR, properties.getProperty(PROPERTY_KEY_WORKDIR, ".")));
  }

  @Override
  public int restarts() {
    return restarts;
  }
  private boolean isChildAlive() {
    try {
      child.exitValue();
      return false;
    } catch(IllegalThreadStateException e) {
      return true;
    }
  }
}
