package com.nitorcreations.willow.deployer;

import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;

import org.apache.commons.beanutils.PropertyUtils;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.nitorcreations.willow.messages.GcInfo;
import com.nitorcreations.willow.messages.JmxMessage;
import com.nitorcreations.willow.messages.ProcessCPU;
import com.nitorcreations.willow.messages.ThreadInfoMessage;
import com.nitorcreations.willow.messages.WebSocketTransmitter;

public class ProcessStatSender extends PlatformStatsSender implements Runnable {
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private final long pid;
  private AtomicBoolean running = new AtomicBoolean(true);
  private final LaunchMethod child;
  private MBeanServerConnection server = null;

  public ProcessStatSender(WebSocketTransmitter transmitter, LaunchMethod child, StatisticsConfig conf) throws IOException {
    super(transmitter, conf);
    this.pid = child.getProcessId();
    this.child = child;
  }

  @Override
  public void run() {
    Sigar sigar = new Sigar();
    long nextProcCpus = System.currentTimeMillis() + conf.getIntervalProcCpus();
    long nextJmx = System.currentTimeMillis() + conf.getIntervalJmx();
    ProcCpu pCStat;
    try (JMXConnector connector = DeployerControl.getJMXConnector(pid)) {
      if (connector != null) {
        server = connector.getMBeanServerConnection();
      }
      while (running.get()) {
        long now = System.currentTimeMillis();
        if (now > nextProcCpus) {
          try {
            pCStat = sigar.getProcCpu(pid);
            ProcessCPU msg = new ProcessCPU();
            PropertyUtils.copyProperties(msg, pCStat);
            msg.addTags("category_processcpu_" + child.getName());
            transmitter.queue(msg);
          } catch (SigarException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogRecord rec = new LogRecord(Level.WARNING, "Failed to Process CPU statistics");
            rec.setThrown(e);
            logger.log(rec);
          }
          nextProcCpus = nextProcCpus + conf.getIntervalProcCpus();
        }
        if (server != null) {
          if (now > nextJmx) {
            try {
              JmxMessage msg = getJmxStats();
              if (msg != null) {
                msg.addTags("category_jmx_" + child.getName());
                transmitter.queue(msg);
              }
            } catch (IOException | MalformedObjectNameException | ReflectionException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstanceNotFoundException | MBeanException e) {
              LogRecord rec = new LogRecord(Level.WARNING, "Failed to get JMX statistics");
              rec.setThrown(e);
              logger.log(rec);
            }
            nextJmx = nextJmx + conf.getIntervalJmx();
          }
        }
        try {
          TimeUnit.MILLISECONDS.sleep(conf.shortest());
        } catch (InterruptedException e) {
          logger.info("Process statistics interrupted");
          return;
        }
      }
    } catch (IOException e) {
      logger.info("Process statistics caught exception: " + e.getMessage());
    }
  }

  public JmxMessage getJmxStats() throws MalformedObjectNameException, IOException, ReflectionException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstanceNotFoundException, MBeanException {
    JmxMessage ret = new JmxMessage();
    Set<String> poolNames = addGc(ret);
    addPools(ret, poolNames);
    addMemory(ret);
    addCodeCache(ret);
    addThreads(ret);
    addUptime(ret);
    addClassloading(ret);
    return ret;
  }

  private Set<String> addGc(JmxMessage ret) throws MalformedObjectNameException, IOException {
    ObjectName query = new ObjectName("java.lang:type=GarbageCollector,*");
    Set<ObjectInstance> gcs = server.queryMBeans(query, null);
    LinkedHashSet<String> poolNames = new LinkedHashSet<String>();
    for (ObjectInstance next : gcs) {
      GcInfo gi = new GcInfo();
      try {
        gi.setName((String) server.getAttribute(next.getObjectName(), "Name"));
        gi.setCollectionCount(((Number) server.getAttribute(next.getObjectName(), "CollectionCount")).longValue());
        gi.setCollectionTime(((Number) server.getAttribute(next.getObjectName(), "CollectionTime")).longValue());
        ret.gcInfo.add(gi);
        String[] poolNameArr = (String[]) server.getAttribute(next.getObjectName(), "MemoryPoolNames");
        for (String nextPool : poolNameArr) {
          if (!poolNames.contains(nextPool)) {
            poolNames.add(nextPool);
          }
        }
      } catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException e) {
        LogRecord rec = new LogRecord(Level.WARNING, "Failed to collect GC JMX statistics");
        rec.setThrown(e);
        logger.log(rec);
      }
    }
    return poolNames;
  }

  private void addPools(JmxMessage ret, Set<String> poolNames) throws MalformedObjectNameException, IOException {
    for (String nextPool : poolNames) {
      ObjectName query = new ObjectName("java.lang:type=MemoryPool,name=" + nextPool);
      Set<ObjectInstance> memPool = server.queryMBeans(query, null);
      ObjectInstance next = memPool.iterator().next();
      try {
        ret.memoryPoolUsage.put(nextPool, (Long) ((CompositeDataSupport) server.getAttribute(next.getObjectName(), "Usage")).get("used"));
        ret.memoryPoolPeakUsage.put(nextPool, (Long) ((CompositeDataSupport) server.getAttribute(next.getObjectName(), "PeakUsage")).get("used"));
      } catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException e) {
        LogRecord rec = new LogRecord(Level.WARNING, "Failed to collect Memory JMX statistics");
        rec.setThrown(e);
        logger.log(rec);
      }
    }
  }

  private void addMemory(JmxMessage ret) throws MalformedObjectNameException, IOException {
    ObjectName query = new ObjectName("java.lang:type=Memory");
    Set<ObjectInstance> mem = server.queryMBeans(query, null);
    ObjectInstance next = mem.iterator().next();
    try {
      ret.setHeapMemory(((Long) ((CompositeDataSupport) server.getAttribute(next.getObjectName(), "HeapMemoryUsage")).get("used")).longValue());
      ret.setNonHeapMemory(((Long) ((CompositeDataSupport) server.getAttribute(next.getObjectName(), "NonHeapMemoryUsage")).get("used")).longValue());
    } catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException e) {
      LogRecord rec = new LogRecord(Level.WARNING, "Failed to collect Heap Memory JMX statistics");
      rec.setThrown(e);
      logger.log(rec);
    }
  }

  private void addCodeCache(JmxMessage ret) throws IOException, MalformedObjectNameException {
    ObjectName query = new ObjectName("java.lang:type=MemoryPool,name=Code Cache");
    Set<ObjectInstance> cc = server.queryMBeans(query, null);
    ObjectInstance next = cc.iterator().next();
    try {
      ret.setHeapMemory(((Long) ((CompositeDataSupport) server.getAttribute(next.getObjectName(), "Usage")).get("used")).longValue());
    } catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException e) {
      LogRecord rec = new LogRecord(Level.WARNING, "Failed to collect Code Cache statistics");
      rec.setThrown(e);
      logger.log(rec);
    }
  }

  private void addThreads(JmxMessage ret) throws IOException, MalformedObjectNameException, InstanceNotFoundException, MBeanException, ReflectionException {
    ObjectName query = new ObjectName("java.lang:type=Threading");
    Set<ObjectInstance> tr = server.queryMBeans(query, null);
    ObjectInstance next = tr.iterator().next();
    ThreadMXBean threadBean = JMX.newMBeanProxy(server, query, ThreadMXBean.class);
    ret.setLiveThreads(threadBean.getThreadCount());
    long[] ids = threadBean.getAllThreadIds();
    for (long nextId : ids) {
      CompositeDataSupport tInfo = (CompositeDataSupport) server.invoke(query, "getThreadInfo", new Object[] { Long.valueOf(nextId) }, new String[] { Long.TYPE.getName() });
      ThreadInfoMessage tim = new ThreadInfoMessage();
      tim.setBlockedCount(((Long) tInfo.get("blockedCount")).longValue());
      tim.setBlockedTime(((Long) tInfo.get("blockedTime")).longValue());
      tim.setInNative(((Boolean) tInfo.get("inNative")).booleanValue());
      tim.setLockName((String) tInfo.get("lockName"));
      tim.setLockOwnerId(((Long) tInfo.get("lockOwnerId")).longValue());
      tim.setLockOwnerName((String) tInfo.get("lockOwnerName"));
      tim.setSuspended(((Boolean) tInfo.get("suspended")).booleanValue());
      tim.setThreadId(((Long) tInfo.get("threadId")).longValue());
      tim.setThreadName((String) tInfo.get("lockOwnerName"));
      tim.setThreadState(Thread.State.valueOf((String) tInfo.get("threadState")));
      tim.setWaitedCount(((Long) tInfo.get("waitedCount")).longValue());
      tim.setWaitedTime(((Long) tInfo.get("waitedTime")).longValue());
      ret.threads.add(tim);
    }
  }

  private void addUptime(JmxMessage ret) throws MalformedObjectNameException {
    ObjectName query = new ObjectName("java.lang:type=Runtime");
    RuntimeMXBean runtimeBean = JMX.newMBeanProxy(server, query, RuntimeMXBean.class);
    ret.setStartTime(runtimeBean.getStartTime());
    ret.setUptime(runtimeBean.getUptime());
  }

  private void addClassloading(JmxMessage ret) throws MalformedObjectNameException {
    ObjectName query = new ObjectName("java.lang:type=ClassLoading");
    ClassLoadingMXBean clBean = JMX.newMBeanProxy(server, query, ClassLoadingMXBean.class);
    ret.setLoadedClassCount(clBean.getLoadedClassCount());
    ret.setUnloadedClassCount(clBean.getUnloadedClassCount());
  }
}
