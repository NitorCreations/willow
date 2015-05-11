package com.nitorcreations.willow.deployer;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.inject.Singleton;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

@Singleton
public class Status extends DeployerControl {
  public static void main(String[] args) {
    injector.getInstance(Status.class).doMain(args);
  }

  public void doMain(String[] args) {
    try {
      List<Long> firstPids;
      if (args.length > 0) {
        firstPids = new ArrayList<>();
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
      log.log(Level.WARNING, "Failed to connect to deployer " + deployerName);
    }
  }
}
