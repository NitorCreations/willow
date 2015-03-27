package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.ENV_DEPLOYER_NAME;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import org.hyperic.sigar.ProcTime;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.ptql.ProcessQuery;
import org.hyperic.sigar.ptql.ProcessQueryFactory;

public class RestartChild extends DeployerControl {
  public static void main(String[] args) {
    new RestartChild().doMain(args);
  }

  public void doMain(String[] args) {
    if (args.length < 1)
      usage("At least one arguments expected: {role}");
    String deployerName = args[0];
    extractNativeLib();
    try {
      Sigar sigar = new Sigar();
      ProcessQuery q = ProcessQueryFactory.getInstance().getQuery("Env." + ENV_DEPLOYER_NAME + ".eq=" + deployerName);
      long minStart = Long.MAX_VALUE;
      long firstPid = 0;
      long[] pids = q.find(sigar);
      if (pids.length > 1) {
        for (long pid : pids) {
          ProcTime time = sigar.getProcTime(pid);
          if (time.getStartTime() < minStart) {
            minStart = time.getStartTime();
            firstPid = pid;
          }
        }
        try (JMXConnector conn = getJMXConnector(firstPid)) {
          MBeanServerConnection server = conn.getMBeanServerConnection();
          MainMBean proxy = JMX.newMBeanProxy(server, OBJECT_NAME, MainMBean.class);
          if (args.length == 1) {
            System.out.println("Restarting all children of " + args[0]);
            proxy.restartChild(null);
          } else {
            for (int i=1; i<args.length;i++) {
              System.out.println("Restarting '" + args[i] + "' child of " + args[0]);
              proxy.restartChild(args[i]);
            }
          }
          System.out.println(proxy.getStatus());
          System.exit(0);
        } catch (Throwable e) {
          log.info("Restart failed");
          e.printStackTrace();
          System.exit(1);
        }
      } else {
        System.out.println("No deployer with role " + deployerName + " running");
      }
    } catch (Throwable e) {
      LogRecord rec = new LogRecord(Level.WARNING, "Failed to connect to deployer " + deployerName);
      log.log(rec);
    }
  }
}
