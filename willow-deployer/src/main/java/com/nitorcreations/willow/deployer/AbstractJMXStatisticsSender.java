package com.nitorcreations.willow.deployer;

import java.util.Properties;

import javax.inject.Inject;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

/**
 * Abstract base class for statistics senders that use JMX to connect to the child process.
 * 
 * @author Mikko Tommila
 */
public abstract class AbstractJMXStatisticsSender extends AbstractStatisticsSender {

  @Inject
  protected Main main;

  private String childName;
  private long oldChildPid = -2;
  private JMXConnector connector;
  private MBeanServerConnection server;

  protected AbstractJMXStatisticsSender() {
  }

  @Override
  public void setProperties(Properties properties) {
    childName = properties.getProperty("childName");
  }

  @Override
  public void stop() {
    closeMBeanServerConnection();
    super.stop();
  }

  /**
   * Return the MBean server connection to the child process.
   * 
   * @return MBean server connection.
   */
  protected MBeanServerConnection getMBeanServerConnection() {
    long childPid = main.getChildPid(getChildName());
    if (childPid > 0 && childPid != oldChildPid) {
      if (connector != null) {
        try {
          connector.close();
        } catch (Exception e) { }
        connector = null;
        server = null;
      }
    }
    if (childPid > 0) {
      try {
        if (connector == null) {
          connector = DeployerControl.getJMXConnector(childPid);
        }
        if (connector != null && server == null) {
          server = connector.getMBeanServerConnection();
          oldChildPid = childPid;
        }
      } catch (Exception e) {
        if (connector != null) {
          try {
            connector.close();
          } catch (Exception e2) { }
          connector = null;
          server = null;
        }
      }
    }
    return server;
  }

  /**
   * Close the MBean server connection.
   */
  protected void closeMBeanServerConnection() {
    if (connector != null) {
      try {
        connector.close();
      } catch (Exception e) { }
      connector = null;
    }
  }

  /**
   * Get the child name.
   * 
   * @return The child name as configured, or the name of the first child by default.
   */
  protected String getChildName() {
    if (childName == null) {
      String[] children = main.getChildNames();
      if (children.length > 0) {
        childName = children[0];
      }
    }
    return childName;
  }
}
