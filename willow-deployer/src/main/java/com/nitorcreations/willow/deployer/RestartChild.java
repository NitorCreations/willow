package com.nitorcreations.willow.deployer;

import java.util.logging.Level;

import javax.inject.Singleton;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Singleton
@SuppressFBWarnings(value={"DM_EXIT"}, justification="cli tool needs to convey correct exit code")
public class RestartChild extends DeployerControl {
  public static void main(String[] args) {
    injector.getInstance(RestartChild.class).doMain(args);
  }

  public void doMain(String[] args) {
    if (args.length < 1)
      usage("At least one arguments expected: {role}");
    String deployerName = args[0];
    try {
      long firstPid = findOldDeployerPid(deployerName);
      if (firstPid > 0) {
        try (JMXConnector conn = getJMXConnector(firstPid)) {
          if (conn == null) {
            log.log(Level.WARNING, "Failed to connect to deployer " + deployerName);
            System.exit(1);
          }
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
        System.exit(2);
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, "Failed to connect to deployer " + deployerName);
      System.exit(1);
    }
  }
}
