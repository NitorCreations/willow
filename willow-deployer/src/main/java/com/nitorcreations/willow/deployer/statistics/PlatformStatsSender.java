package com.nitorcreations.willow.deployer.statistics;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.beanutils.PropertyUtils;
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.ProcStat;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;

import com.nitorcreations.willow.messages.CPU;
import com.nitorcreations.willow.messages.DiskIO;
import com.nitorcreations.willow.messages.DiskUsage;
import com.nitorcreations.willow.messages.Memory;
import com.nitorcreations.willow.messages.NetInterface;
import com.nitorcreations.willow.messages.OsInfo;
import com.nitorcreations.willow.messages.Processes;
import com.nitorcreations.willow.messages.TcpInfo;

@Named("platform")
public class PlatformStatsSender extends AbstractStatisticsSender {
  @Inject
  protected SigarProxy sigar;
  protected StatisticsConfig conf;
  private long nextProcs;
  private long nextCpus;
  private long nextMem;
  private long nextDisks;
  private long nextNet;
  private long nextNetStat;
  private long nextDiskIO;
  private long nextOs;

  @Override
  public void stop() {
    synchronized (this) {
      running.set(false);
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
        logger.log(Level.WARNING, "Failed to get Process statistics", e);
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
        logger.log(Level.WARNING, "Failed to get CPU statistics", e);
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
        logger.log(Level.WARNING, "Failed to get CPU statistics", e);
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
        logger.log(Level.WARNING, "Failed to get Memory statistics", e);
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
            } catch (SigarException e) {
              logger.fine("Failed to get disk usage for " +  nextFs.getDirName());
            }
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
        logger.log(Level.WARNING, "Failed to get Disk statistics", e);
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
        logger.log(Level.WARNING, "Failed to get Disk statistics", e);
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
        logger.log(Level.WARNING, "Failed to get Disk statistics", e);
      }
      nextNet = nextNet + conf.getIntervalNet();
      try {
        TimeUnit.MILLISECONDS.sleep(conf.shortest());
      } catch (InterruptedException e) {}
    }
    if (now > nextOs) {
      try {
        OperatingSystem os = OperatingSystem.getInstance();
        OsInfo osInfo = new OsInfo();
        PropertyUtils.copyProperties(osInfo, os);
        transmitter.queue(osInfo);
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        logger.log(Level.WARNING, "Failed to get os info", e);
      }
      nextOs = nextOs + conf.getIntervalOs();
    }
  }

  @Override
  public void setProperties(Properties properties) {
    conf = new StatisticsConfig(properties);
    long now = System.currentTimeMillis();
    nextProcs = now + TimeUnit.SECONDS.toMillis(2);
    nextCpus = now + TimeUnit.SECONDS.toMillis(2);
    nextMem = now + TimeUnit.SECONDS.toMillis(2);
    nextDisks = now + TimeUnit.SECONDS.toMillis(2);
    nextNet = now + TimeUnit.SECONDS.toMillis(2);
    nextNetStat = now + TimeUnit.SECONDS.toMillis(2);
    nextDiskIO = now + TimeUnit.SECONDS.toMillis(2);
    nextOs = now + TimeUnit.SECONDS.toMillis(1);
  }
}
