package com.nitorcreations.willow.deployer.statistics;

import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import com.nitorcreations.willow.messages.GcInfo;
import com.nitorcreations.willow.messages.JmxMessage;

@Named("jmx")
public class JMXStatsSender extends AbstractJMXStatisticsSender {
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private long nextJmx;
  private StatisticsConfig conf;

  @Override
  public void setProperties(Properties properties) {
    super.setProperties(properties);
    conf = new StatisticsConfig(properties);
    nextJmx = System.currentTimeMillis() + conf.getIntervalJmx();
  }
  @Override
  public void execute() {
    MBeanServerConnection server = getMBeanServerConnection();
    long now = System.currentTimeMillis();
    if (server != null && now > nextJmx) {
      try {
        JmxMessage msg = getJmxStats();
        msg.addTags("category_jmx_" + getChildName());
        transmitter.queue(msg);
      } catch (IOException | MalformedObjectNameException | ReflectionException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstanceNotFoundException | MBeanException e) {
        logger.log(Level.WARNING, "Failed to get JMX statistics", e);
      }
      nextJmx = nextJmx + conf.getIntervalJmx();
    }
    try {
      TimeUnit.MILLISECONDS.sleep(conf.shortest());
    } catch (InterruptedException e) {
      logger.info("Process statistics interrupted");
      return;
    }
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
    MBeanServerConnection server = getMBeanServerConnection();
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
        logger.log(Level.WARNING, "Failed to collect GC JMX statistics", e);
      }
    }
    return poolNames;
  }

  private void addPools(JmxMessage ret, Set<String> poolNames) throws MalformedObjectNameException, IOException {
    MBeanServerConnection server = getMBeanServerConnection();
    for (String nextPool : poolNames) {
      ObjectName query = new ObjectName("java.lang:type=MemoryPool,name=" + nextPool);
      Set<ObjectInstance> memPool = server.queryMBeans(query, null);
      ObjectInstance next = memPool.iterator().next();
      try {
        ret.memoryPoolUsage.put(nextPool, (Long) ((CompositeDataSupport) server.getAttribute(next.getObjectName(), "Usage")).get("used"));
        ret.memoryPoolPeakUsage.put(nextPool, (Long) ((CompositeDataSupport) server.getAttribute(next.getObjectName(), "PeakUsage")).get("used"));
      } catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException e) {
        logger.log(Level.WARNING, "Failed to collect Memory JMX statistics", e);
      }
    }
  }

  private void addMemory(JmxMessage ret) throws MalformedObjectNameException, IOException {
    MBeanServerConnection server = getMBeanServerConnection();
    ObjectName query = new ObjectName("java.lang:type=Memory");
    Set<ObjectInstance> mem = server.queryMBeans(query, null);
    ObjectInstance next = mem.iterator().next();
    try {
      ret.setHeapMemory(((Long) ((CompositeDataSupport) server.getAttribute(next.getObjectName(), "HeapMemoryUsage")).get("used")).longValue());
      ret.setNonHeapMemory(((Long) ((CompositeDataSupport) server.getAttribute(next.getObjectName(), "NonHeapMemoryUsage")).get("used")).longValue());
    } catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException e) {
      logger.log(Level.WARNING, "Failed to collect Heap Memory JMX statistics", e);
    }
  }

  private void addCodeCache(JmxMessage ret) throws IOException, MalformedObjectNameException {
    MBeanServerConnection server = getMBeanServerConnection();
    ObjectName query = new ObjectName("java.lang:type=MemoryPool,name=Code Cache");
    Set<ObjectInstance> cc = server.queryMBeans(query, null);
    ObjectInstance next = cc.iterator().next();
    try {
      ret.setHeapMemory(((Long) ((CompositeDataSupport) server.getAttribute(next.getObjectName(), "Usage")).get("used")).longValue());
    } catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException e) {
      logger.log(Level.WARNING, "Failed to collect Code Cache statistics");
    }
  }

  private void addUptime(JmxMessage ret) throws MalformedObjectNameException {
    MBeanServerConnection server = getMBeanServerConnection();
    ObjectName query = new ObjectName("java.lang:type=Runtime");
    RuntimeMXBean runtimeBean = JMX.newMBeanProxy(server, query, RuntimeMXBean.class);
    ret.setStartTime(runtimeBean.getStartTime());
    ret.setUptime(runtimeBean.getUptime());
  }

  private void addClassloading(JmxMessage ret) throws MalformedObjectNameException {
    MBeanServerConnection server = getMBeanServerConnection();
    ObjectName query = new ObjectName("java.lang:type=ClassLoading");
    ClassLoadingMXBean clBean = JMX.newMBeanProxy(server, query, ClassLoadingMXBean.class);
    ret.setLoadedClassCount(clBean.getLoadedClassCount());
    ret.setUnloadedClassCount(clBean.getUnloadedClassCount());
  }

}
