package com.nitorcreations.willow.deployer.statistics;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.beanutils.PropertyUtils;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;

import com.nitorcreations.willow.messages.ProcessCPU;

@Named("process")
public class ProcessStatSender extends AbstractChildStatisticsSender {
  @Inject
  protected SigarProxy sigar;
  private StatisticsConfig conf;
  private long nextProcCpus;


  @Override
  public void execute() {
    if (conf == null) {
      throw new IllegalStateException("Tried to start unconfigured statistics sender");
    }
    ProcCpu pCStat;
    long now = System.currentTimeMillis();
    if (now > nextProcCpus) {
      for (String childName : getChildren()) {
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
          logger.log(Level.WARNING, "Failed to Process CPU statistics", e);
        }
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
    super.setProperties(properties);
    conf = new StatisticsConfig(properties);
    nextProcCpus = System.currentTimeMillis() + conf.getIntervalProcCpus();
  }
}
