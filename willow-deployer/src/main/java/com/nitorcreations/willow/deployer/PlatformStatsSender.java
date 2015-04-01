package com.nitorcreations.willow.deployer;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.beanutils.PropertyUtils;
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.ProcStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.nitorcreations.willow.messages.CPU;
import com.nitorcreations.willow.messages.DiskIO;
import com.nitorcreations.willow.messages.DiskUsage;
import com.nitorcreations.willow.messages.Memory;
import com.nitorcreations.willow.messages.NetInterface;
import com.nitorcreations.willow.messages.Processes;
import com.nitorcreations.willow.messages.TcpInfo;
import com.nitorcreations.willow.messages.WebSocketTransmitter;

@Named("platform")
public class PlatformStatsSender extends AbstractStatisticsSender implements StatisticsSender {
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private AtomicBoolean running = new AtomicBoolean(true);
  private Sigar sigar;
  @Inject
  protected WebSocketTransmitter transmitter;
  protected StatisticsConfig conf;
  private long nextProcs;
  private long nextCpus;
  private long nextMem;
  private long nextDisks;
  private long nextNet;
  private long nextNetStat;
  private long nextDiskIO;

  public void stop() {
    running.set(false);
    synchronized (this) {
      this.notifyAll();
    }
  }

  @Override
  public void execute() {
    ProcStat pStat;
    DiskUsage[] dStat;
    Map<String, DiskIO> dIO = new HashMap<>();
    Cpu cStat;
    Mem mem;
    NetStat netStat;
    long now = System.currentTimeMillis();
    FileSystem[] fileSystems;
    if (now > nextProcs) {
      try {
        pStat = sigar.getProcStat();
        Processes msg = new Processes();
        PropertyUtils.copyProperties(msg, pStat);
        transmitter.queue(msg);
      } catch (SigarException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        LogRecord rec = new LogRecord(Level.WARNING, "Failed to get Process statistics");
        rec.setThrown(e);
        logger.log(rec);
      }
      nextProcs = nextProcs + conf.getIntervalProcs();
    }
    if (now > nextCpus) {
      try {
        cStat = sigar.getCpu();
        CPU msg = new CPU();
        PropertyUtils.copyProperties(msg, cStat);
        transmitter.queue(msg);
      } catch (SigarException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        LogRecord rec = new LogRecord(Level.WARNING, "Failed to get CPU statistics");
        rec.setThrown(e);
        logger.log(rec);
      }
      nextCpus = nextCpus + conf.getIntervalCpus();
    }
    if (now > nextNetStat) {
      try {
        netStat = sigar.getNetStat();
        TcpInfo msg = new TcpInfo();
        PropertyUtils.copyProperties(msg, netStat);
        transmitter.queue(msg);
      } catch (SigarException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        LogRecord rec = new LogRecord(Level.WARNING, "Failed to get CPU statistics");
        rec.setThrown(e);
        logger.log(rec);
      }
      nextNetStat = nextNetStat + conf.getIntervalNetStat();
    }
    if (now > nextMem) {
      try {
        mem = sigar.getMem();
        Memory msg = new Memory();
        PropertyUtils.copyProperties(msg, mem);
        transmitter.queue(msg);
      } catch (SigarException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        LogRecord rec = new LogRecord(Level.WARNING, "Failed to get Memory statistics");
        rec.setThrown(e);
        logger.log(rec);
      }
      nextMem = nextMem + conf.getIntervalMem();
    }
    if (now > nextDiskIO) {
      try {
        fileSystems = sigar.getFileSystemList();
        dIO.clear();
        for (FileSystem nextFs : fileSystems) {
          if (!dIO.containsKey(nextFs.getDevName())) {
            org.hyperic.sigar.DiskUsage next = null;
            try {
              next = sigar.getDiskUsage(nextFs.getDevName());
            } catch (SigarException e) {}
            if (next != null) {
              DiskIO nextMsg = new DiskIO();
              PropertyUtils.copyProperties(nextMsg, next);
              nextMsg.setName(nextFs.getDirName());
              nextMsg.setDevice(nextFs.getDevName());
              dIO.put(nextMsg.getDevice(), nextMsg);
            }
          }
        }
        for (DiskIO next : dIO.values()) {
          transmitter.queue(next);
        }
      } catch (SigarException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        LogRecord rec = new LogRecord(Level.WARNING, "Failed to get Disk statistics");
        rec.setThrown(e);
        logger.log(rec);
      }
      nextDiskIO = nextDiskIO + conf.getIntervalDiskIO();
    }
    if (now > nextDisks) {
      try {
        fileSystems = sigar.getFileSystemList();
        dStat = new DiskUsage[fileSystems.length];
        for (int i = 0; i < fileSystems.length; i++) {
          FileSystem fileSystem = fileSystems[i];
          dStat[i] = new DiskUsage();
          if (fileSystem.getType() != FileSystem.TYPE_CDROM && fileSystem.getType() != FileSystem.TYPE_NETWORK) {
            FileSystemUsage next = sigar.getMountedFileSystemUsage(fileSystem.getDirName());
            PropertyUtils.copyProperties(dStat[i], next);
          }
          dStat[i].setName(fileSystem.getDirName());
        }
        for (DiskUsage next : dStat) {
          transmitter.queue(next);
        }
      } catch (SigarException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        LogRecord rec = new LogRecord(Level.WARNING, "Failed to get Disk statistics");
        rec.setThrown(e);
        logger.log(rec);
      }
      nextDisks = nextDisks + conf.getIntervalDisks();
    }
    if (now > nextNet) {
      try {
        for (String iface : sigar.getNetInterfaceList()) {
          NetInterfaceStat stat = sigar.getNetInterfaceStat(iface);
          NetInterface net = new NetInterface();
          net.setName(iface);
          PropertyUtils.copyProperties(net, stat);
          transmitter.queue(net);
        }
      } catch (SigarException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        LogRecord rec = new LogRecord(Level.WARNING, "Failed to get Disk statistics");
        rec.setThrown(e);
        logger.log(rec);
      }
      nextNet = nextNet + conf.getIntervalNet();
      try {
        TimeUnit.MILLISECONDS.sleep(conf.shortest());
      } catch (InterruptedException e) {}
    }
  }

  @Override
  public void setProperties(Properties properties) {
    sigar = new Sigar();
    conf = new StatisticsConfig(properties);
    nextProcs = System.currentTimeMillis() + conf.getIntervalProcs();
    nextCpus = System.currentTimeMillis() + conf.getIntervalCpus();
    nextMem = System.currentTimeMillis() + conf.getIntervalMem();
    nextDisks = System.currentTimeMillis() + conf.getIntervalDisks();
    nextNet = System.currentTimeMillis() + conf.getIntervalNet();
    nextNetStat = System.currentTimeMillis() + conf.getIntervalNetStat();
    nextDiskIO = System.currentTimeMillis() + conf.getIntervalDiskIO();
  }
}
