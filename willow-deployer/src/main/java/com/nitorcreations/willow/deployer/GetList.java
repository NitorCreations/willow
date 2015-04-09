package com.nitorcreations.willow.deployer;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class GetList extends DeployerControl {
  public static void main(String[] args) {
    injector.getInstance(GetList.class).doMain(args);
  }

  public void doMain(String[] args) {
    try {
      Set<Long> firstPids;
      if (args.length > 0) {
        firstPids = new HashSet<>();
        for (String next : args) {
          long nextPid = findOldDeployerPid(next);
          if (nextPid > 0) {
            firstPids.add(nextPid);
          }
        }
      } else {
       firstPids = findOldDeployerPids();
      }
      for (long next : firstPids) {
        try (JMXConnector conn = getJMXConnector(next)) {
          MBeanServerConnection server = conn.getMBeanServerConnection();
          MainMBean proxy = JMX.newMBeanProxy(server, OBJECT_NAME, MainMBean.class);
          System.out.println(proxy.getStatus());
          System.exit(0);
        } catch (Throwable e) {
          log.info("JMX Status failed");
          e.printStackTrace();
          System.exit(1);
        }
      }
    } catch (Throwable e) {
      LogRecord rec = new LogRecord(Level.WARNING, "Failed to connect to deployer " + deployerName);
      log.log(rec);
    }
  }
}
