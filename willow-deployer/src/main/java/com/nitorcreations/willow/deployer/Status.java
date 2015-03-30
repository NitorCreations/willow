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

public class Status extends DeployerControl {
  public static void main(String[] args) {
    new Status().doMain(args);
  }

  public void doMain(String[] args) {
    if (args.length < 1)
      usage("At least one arguments expected: {role}");
    String deployerName = args[0];
    extractNativeLib();
    try {
      long firstPid = findOldDeployerPid(deployerName);
      if (firstPid > 0) {
        try (JMXConnector conn = getJMXConnector(firstPid)) {
          MBeanServerConnection server = conn.getMBeanServerConnection();
          MainMBean proxy = JMX.newMBeanProxy(server, OBJECT_NAME, MainMBean.class);
          System.out.println(proxy.getStatus());
          System.exit(0);
        } catch (Throwable e) {
          log.info("JMX Status failed");
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
