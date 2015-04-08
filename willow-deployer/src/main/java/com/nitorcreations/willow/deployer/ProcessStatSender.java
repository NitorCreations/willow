package com.nitorcreations.willow.deployer;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.beanutils.PropertyUtils;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.nitorcreations.willow.messages.ProcessCPU;
import com.nitorcreations.willow.messages.WebSocketTransmitter;

@Named("process")
public class ProcessStatSender extends AbstractStatisticsSender {
  private Logger logger = Logger.getLogger(this.getClass().getName());
  Sigar sigar;
  private String childName;
  private StatisticsConfig conf;
  @Inject
  protected WebSocketTransmitter transmitter;
  @Inject
  protected Main main;
  private long nextProcCpus;


  @Override
  public void execute() {
    ProcCpu pCStat;
    long now = System.currentTimeMillis();
    if (now > nextProcCpus) {
      try {
        long pid = main.getChildPid(childName);
        if (pid > 0) {
          pCStat = sigar.getProcCpu(pid);
          ProcessCPU msg = new ProcessCPU();
          PropertyUtils.copyProperties(msg, pCStat);
          msg.addTags("category_processcpu_" + childName);
          transmitter.queue(msg);
        }
      } catch (SigarException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        LogRecord rec = new LogRecord(Level.WARNING, "Failed to Process CPU statistics");
        rec.setThrown(e);
        logger.log(rec);
      }
      nextProcCpus = nextProcCpus + conf.getIntervalProcCpus();
    }
    try {
      TimeUnit.MILLISECONDS.sleep(conf.shortest());
    } catch (InterruptedException e) {
      logger.info("Process statistics interrupted");
      return;
    }
  }

  @Override
  public void setProperties(Properties properties) {
    conf = new StatisticsConfig(properties);
    nextProcCpus = System.currentTimeMillis() + conf.getIntervalProcCpus();
    childName = properties.getProperty("childName");
    if (childName == null) {
      String[] children = main.getChildNames();
      if (children.length > 0) {
        childName = children[0];
      }
    }
  }
}
