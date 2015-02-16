package com.nitorcreations.willow.deployer;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.management.JMX;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.apache.commons.beanutils.ConvertUtils;
import org.hyperic.sigar.Sigar;

public class JMXOperation extends DeployerControl {
  public static void main(String[] args) {
    new JMXOperation().doMain(args);
  }
  public void doMain(String[] args) {
    if (args.length < 3) {
      usage("Usage: JMXOperation {pid|deployerName [childName]} objectName operationName [arguments...]");
    }
    List<String> argList = Arrays.asList(args);
    String first = argList.remove(0);
    JMXConnector conn = null;
    boolean directPid = false;
    try {
      if (first.matches("\\d+")) {
        conn = getJMXConnector(Long.parseLong(first));
      }
      if (conn == null) {
        deployerName = first;
        Sigar sigar = new Sigar();
        long mypid = sigar.getPid();
        if (mypid <= 0) {
          LogRecord rec = new LogRecord(Level.WARNING, "Failed resolve own pid");
          log.log(rec);
          System.exit(1);
        }
        long firstPid = findOldDeployerPid(deployerName);
        if (firstPid <= 0) {
          LogRecord rec = new LogRecord(Level.WARNING, "Failed to find pid for deployer " + deployerName);
          log.log(rec);
          System.exit(1);
        }
        conn = getJMXConnector(firstPid);
      } else {
        directPid = true;
      }
      if (conn == null) {
        LogRecord rec = new LogRecord(Level.WARNING, "Failed to connect to deployer " + deployerName);
        log.log(rec);
        System.exit(1);
      }
      MBeanServerConnection server = conn.getMBeanServerConnection();
      if (!directPid) {
        MainMBean proxy = JMX.newMBeanProxy(server, OBJECT_NAME, MainMBean.class);
        long childPid = proxy.getChildPid(argList.get(0));
        if (childPid > 0) {
          conn = getJMXConnector(childPid);
          if (conn == null) {
            LogRecord rec = new LogRecord(Level.WARNING, "Failed to connect to deployer child" + deployerName
              + "::"  + argList.get(0));
            log.log(rec);
            System.exit(1);
          }
          argList.remove(0);

        }
      }    
      ObjectName objectName = new ObjectName(argList.remove(0));
      String operationName = argList.remove(0);
      MBeanInfo mBeanInfo;
      mBeanInfo = server.getMBeanInfo(objectName);
      MBeanOperationInfo[] operations = mBeanInfo.getOperations();
      for (MBeanOperationInfo operation : operations) {
        if (operation.getName().equals(operationName)) {
          try {
            MBeanParameterInfo[] parameters = operation.getSignature();
            String[] signature = new String[parameters.length];
            for (int i = 0; i < signature.length; i++) {
              signature[i] = parameters[i].getType();
            }
            Object[] params = argList.toArray(new Object[argList.size()]);
            if (params.length != signature.length) {
              continue;
            }
            for (int i = 0; i < signature.length; i++) {
              params[i] = ConvertUtils.convert(params[i], Class.forName(signature[i]));
            }
            Object result = server.invoke(objectName, operationName, params, signature);
            if (!operation.getReturnType().equals("void")) {
              System.out.println(result);
            }
            System.exit(0);
          } catch (Throwable e) {
            log.info("JMX Operation failed");
            System.exit(1);
          }
        }
      }
      System.out.println("No operation found with name " + operationName + " and " + argList.size() + " argument(s)");
    } catch (Throwable e) {
      LogRecord rec = new LogRecord(Level.WARNING, "Failed to connect to deployer " + deployerName);
      rec.setThrown(e);
      log.log(rec);
      System.exit(1);
    } finally {
      if (conn != null) {
        try {
          conn.close();
        } catch (IOException e) {
          log.fine("Failed to close JMXConnection");
        }
      }
    }
  }
}
