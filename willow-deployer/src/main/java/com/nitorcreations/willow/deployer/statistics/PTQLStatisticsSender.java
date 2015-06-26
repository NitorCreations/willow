package com.nitorcreations.willow.deployer.statistics;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.inject.Named;

import org.apache.commons.beanutils.PropertyUtils;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.MalformedQueryException;
import org.hyperic.sigar.ptql.ProcessQuery;
import org.hyperic.sigar.ptql.ProcessQueryFactory;

import com.nitorcreations.willow.messages.ProcessCPU;

@Named("ptql")
public class PTQLStatisticsSender extends AbstractStatisticsSender {
  protected Sigar sigar = new Sigar();
  private ProcessQuery query;
  private long nextProcCpus = System.currentTimeMillis();
  private long interval = - 1;

  @Override
  public void setProperties(Properties properties) {
    String q = properties.getProperty("query");
    if (q == null) throw new IllegalArgumentException("Must define a query");
    interval = Long.parseLong(properties.getProperty("interval", "5000"));
    try {
      query = ProcessQueryFactory.getInstance().getQuery(q);
    } catch (MalformedQueryException e) {
      throw new IllegalArgumentException("Invalid query", e);
    }
  }


  @Override
  public void execute() {
    if (query == null) {
      throw new IllegalStateException("Tried to start unconfigured statistics sender");
    }
    ProcCpu pCStat;
    long now = System.currentTimeMillis();
    if (now > nextProcCpus) {
      try {
        for (long pid : query.find(sigar)) {
          if (pid > 0) {
            pCStat = sigar.getProcCpu(pid);
            String pName = sigar.getProcState(pid).getName().replaceAll("\\W", "_");
            ProcessCPU msg = new ProcessCPU();
            PropertyUtils.copyProperties(msg, pCStat);
            msg.addTags("category_processcpu_" + pName + "_" + pid);
            transmitter.queue(msg);
          }
        }
      } catch (SigarException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        logger.log(Level.WARNING, "Failed to Process CPU statistics", e);
      }
      nextProcCpus = nextProcCpus + interval;
    }
    try {
      TimeUnit.MILLISECONDS.sleep(interval);
    } catch (InterruptedException e) {
      logger.info("Process statistics interrupted");
      return;
    }
  }

}
