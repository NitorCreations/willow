package com.nitorcreations.willow.deployer.statistics;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import com.nitorcreations.willow.deployer.DeployerControl;

/**
 * Abstract base class for statistics senders that use JMX to connect to the child process.
 *
 * @author Mikko Tommila
 */
public abstract class AbstractJMXStatisticsSender extends AbstractChildStatisticsSender {
  private static class MBeanConnectionInfo {
    public JMXConnector connector = null;
    public MBeanServerConnection server = null;
    long childPid = -2;
  }
  protected Map<String, MBeanConnectionInfo> connections = new HashMap<>();

  protected AbstractJMXStatisticsSender() {
  }

  @Override
  public void stop() {
    for (String childName : connections.keySet()) {
      closeMBeanServerConnection(childName);
    }
    super.stop();
  }

  /**
   * Return the MBean server connection to the child process.
   *
   * @return MBean server connection.
   */
  protected MBeanServerConnection getMBeanServerConnection(String childName) {
    MBeanConnectionInfo connInfo = connections.get(childName);
    if (connInfo == null) {
      connInfo = new MBeanConnectionInfo();
      connections.put(childName, connInfo);
    }
    long childPid = main.getFirstJavaChildPid(childName);
    if (childPid > 0 && childPid != connInfo.childPid && connInfo.connector != null) {
      try {
        connInfo.connector.close();
      } catch (IOException e) {
        logger.log(Level.FINE, "Failed to close JMXConnector", e);
      }
      connInfo.connector = null;
      connInfo.server = null;
    }
    if (childPid > 0) {
      try {
        if (connInfo.connector == null) {
          connInfo.connector = DeployerControl.getJMXConnector(childPid);
        }
        if (connInfo.connector != null && connInfo.server == null) {
          connInfo.server = connInfo.connector.getMBeanServerConnection();
          connInfo.childPid = childPid;
        }
      } catch (IOException e) {
        logger.log(Level.FINE, "Failed to get JMX connection", e);
        try {
          connInfo.connector.close();
        } catch (Exception e2) {
          logger.log(Level.FINE, "Failed to close JMXConnector", e2);
        }
        connInfo.connector = null;
        connInfo.server = null;
      }
    }
    return connInfo.server;
  }

  /**
   * Close the MBean server connection.
   */
  protected void closeMBeanServerConnection(String childName) {
    MBeanConnectionInfo connInfo = connections.get(childName);
    if (connInfo != null && connInfo.connector != null) {
      try {
        connInfo.connector.close();
      } catch (IOException e) {
        logger.log(Level.FINE, "Failed to close JMXConnector", e);
      }
      connInfo.connector = null;
    }
  }
}
