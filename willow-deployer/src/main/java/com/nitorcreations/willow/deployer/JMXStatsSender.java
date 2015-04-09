package com.nitorcreations.willow.deployer;

import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
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

import com.nitorcreations.willow.messages.GcInfo;
import com.nitorcreations.willow.messages.JmxMessage;
import com.nitorcreations.willow.messages.ThreadInfoMessage;
import com.nitorcreations.willow.messages.WebSocketTransmitter;

@Named("jmx")
public class JMXStatsSender extends AbstractStatisticsSender implements StatisticsSender {
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private long nextJmx;
  private StatisticsConfig conf;
  @Inject
  protected WebSocketTransmitter transmitter;
  @Inject
  protected Main main;
  private String childName;
  private long oldChildPid = -2;
  private MBeanServerConnection server;
  private JMXConnector connector;

  @Override
  public void setProperties(Properties properties) {
    conf = new StatisticsConfig(properties);
    nextJmx = System.currentTimeMillis() + conf.getIntervalJmx();
    childName = properties.getProperty("childName");
  }
  @Override
  public void execute() {
    long childPid = main.getChildPid(getChildName());
    if (childPid > 0 && childPid != oldChildPid) {
      if (connector != null) {
        try {
          connector.close();
        } catch (IOException e) { }
        connector = null;
      }
    }
    if (childPid > 0) {
      try {
        if (connector == null) {
          connector = DeployerControl.getJMXConnector(childPid);
        }
        if (connector != null) {
          oldChildPid = childPid;
          server = connector.getMBeanServerConnection();
          long now = System.currentTimeMillis();
          if (server != null) {
            if (now > nextJmx) {
              try {
                JmxMessage msg = getJmxStats();
                if (msg != null) {
                  msg.addTags("category_jmx_" + childName);
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
        }
        if (!running.get()) throw new Exception();
      } catch (Exception e) {
        if (connector != null) {
          try {
            connector.close();
          } catch (IOException e1) { }
          connector = null;
        }
      }
    }
    try {
      TimeUnit.MILLISECONDS.sleep(conf.shortest());
    } catch (InterruptedException e) {
      logger.info("Process statistics interrupted");
      return;
    }
  }
  private String getChildName() {
    if (childName == null) {
      String[] children = main.getChildNames();
      if (children.length > 0) {
        childName = children[0];
      }
    }
    return childName;
  }
  public JmxMessage getJmxStats() throws MalformedObjectNameException, IOException, ReflectionException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstanceNotFoundException, MBeanException {
    JmxMessage ret = new JmxMessage();
    Set<String> poolNames = addGc(ret);
    addPools(ret, poolNames);
    addMemory(ret);
    addCodeCache(ret);
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
