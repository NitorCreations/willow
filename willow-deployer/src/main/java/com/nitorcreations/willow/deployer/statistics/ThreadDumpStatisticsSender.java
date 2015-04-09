package com.nitorcreations.willow.deployer.statistics;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Named;
import javax.management.MBeanServerConnection;

import com.nitorcreations.willow.messages.ThreadDumpMessage;

/**
 * Statistics sender for sending thread dumps.
 * Takes a thread dump of the child (via JMX) at specified intervals
 * (default 1 second) and sends it as JSON to the statistics server.
 * 
 * @author Mikko Tommila
 */
@Named("threaddump")
public class ThreadDumpStatisticsSender extends AbstractJMXStatisticsSender {
  private Logger logger = Logger.getLogger(getClass().getName());
  private long interval;

  @Override
  public void setProperties(Properties properties) {
    super.setProperties(properties);
    this.interval = Long.parseLong(properties.getProperty("interval", "1000"));
  }

  @Override
  public void execute() {
    try {
      Thread.sleep(this.interval);
      ThreadMXBean threadMXBean = getThreadMXBean();
      ThreadInfo[] threadInfo = threadMXBean.dumpAllThreads(threadMXBean.isObjectMonitorUsageSupported(), threadMXBean.isSynchronizerUsageSupported());
      ThreadDumpMessage threadDumpMessage = new ThreadDumpMessage(threadInfo);
      threadDumpMessage.addTags("category_threaddump_" + getChildName());
      transmitter.queue(threadDumpMessage);
    } catch (InterruptedException ie) {
      logger.log(Level.INFO, "Sleep was interrupted", ie);
    } catch (IOException ie) {
      logger.log(Level.INFO, "Cannot get thread management bean of child process", ie);
    }
    if (!running.get()) {
      closeMBeanServerConnection();
    }
  }

  private ThreadMXBean getThreadMXBean() throws IOException {
    MBeanServerConnection connection = getMBeanServerConnection();
    ThreadMXBean threadMXBean = ManagementFactory.newPlatformMXBeanProxy(connection , ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
    return threadMXBean;
  }
}
