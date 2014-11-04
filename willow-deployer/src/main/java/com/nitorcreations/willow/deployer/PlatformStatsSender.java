package com.nitorcreations.willow.deployer;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.beanutils.PropertyUtils;
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.ProcCpu;
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

public class PlatformStatsSender implements Runnable {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private AtomicBoolean running = new AtomicBoolean(true);
	protected final WebSocketTransmitter transmitter;
	protected final StatisticsConfig conf;
	
	public PlatformStatsSender(WebSocketTransmitter transmitter, StatisticsConfig config) {
		this.transmitter = transmitter;
		this.conf = config;
	}

	public void stop() {
		running.set(false);
		synchronized (this) {
			this.notifyAll();
		}
	}

	@Override
	public void run() {
		Sigar sigar = new Sigar();

		long nextProcs = System.currentTimeMillis() + conf.getIntervalProcs();
		long nextCpus =  System.currentTimeMillis() + conf.getIntervalCpus();
		long nextMem =  System.currentTimeMillis() + conf.getIntervalMem();
		long nextDisks = System.currentTimeMillis() + conf.getIntervalDisks();
		long nextNet = System.currentTimeMillis() + conf.getIntervalNet();
		long nextNetStat = System.currentTimeMillis() + conf.getIntervalNetStat();
		long nextDiskIO =  System.currentTimeMillis() + conf.getIntervalDiskIO();
		ProcStat pStat;
		DiskUsage[] dStat;
		Map<String, DiskIO> dIO = new HashMap<>();
		Cpu cStat;
		Mem mem;
		NetStat netStat;
		while (running.get()) {
			long now = System.currentTimeMillis();
			FileSystem[] fileSystems;
			if (now > nextProcs) {
				try {
					pStat = sigar.getProcStat();
					Processes msg = new Processes();
					PropertyUtils.copyProperties(msg, pStat);
					transmitter.queue(msg);
				} catch (SigarException | IllegalAccessException | InvocationTargetException | 
						NoSuchMethodException e) {
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
				} catch (SigarException | IllegalAccessException | InvocationTargetException | 
						NoSuchMethodException e) {
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
				} catch (SigarException | IllegalAccessException | InvocationTargetException | 
						NoSuchMethodException e) {
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
				} catch (SigarException | IllegalAccessException | InvocationTargetException | 
						NoSuchMethodException e) {
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
							} catch (SigarException e) {
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
				} catch (SigarException | IllegalAccessException | InvocationTargetException | 
						NoSuchMethodException e) {
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
					for (int i=0; i < fileSystems.length; i++) {
						FileSystemUsage next = sigar.getMountedFileSystemUsage(fileSystems[i].getDirName());
						dStat[i] = new DiskUsage();
						PropertyUtils.copyProperties(dStat[i], next);
						dStat[i].setName(fileSystems[i].getDirName());
					}
					for (DiskUsage next : dStat) {
						transmitter.queue(next);
					}
				} catch (SigarException | IllegalAccessException | InvocationTargetException | 
						NoSuchMethodException e) {
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
				} catch (SigarException | IllegalAccessException | InvocationTargetException | 
						NoSuchMethodException e) {
					LogRecord rec = new LogRecord(Level.WARNING, "Failed to get Disk statistics");
					rec.setThrown(e);
					logger.log(rec);
				}
				nextNet = nextNet + conf.getIntervalNet();
			}
			try {
				TimeUnit.MILLISECONDS.sleep(conf.shortest());
			} catch (InterruptedException e) {
			}
		}
	}
}
