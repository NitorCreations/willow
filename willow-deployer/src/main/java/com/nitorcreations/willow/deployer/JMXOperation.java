package com.nitorcreations.willow.deployer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.inject.Singleton;
import javax.management.Attribute;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang3.ClassUtils;
import org.hyperic.sigar.SigarException;

import com.google.gson.Gson;

@Singleton
public class JMXOperation extends DeployerControl {
  public static void main(String[] args) {
    injector.getInstance(JMXOperation.class).doMain(args);
  }
  public void doMain(String[] args) {
    if (args.length < 3) {
      usage("Usage: JMXOperation {pid|deployerName [childName]} objectName operationName [arguments...]");
    }
    List<String> argList = new ArrayList<>(Arrays.asList(args));
    try (JMXConnector conn = resoveConnection(argList)) {
      MBeanServerConnection server = conn.getMBeanServerConnection();
      ObjectName objectName = new ObjectName(argList.remove(0));
      String operationName = argList.remove(0);
      MBeanInfo mBeanInfo = server.getMBeanInfo(objectName);
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
      for (MBeanAttributeInfo attr : mBeanInfo.getAttributes()) {
        if (attr.getName().equals(operationName)) {
          if (argList.size() == 0) {
            System.out.println(new Gson().toJson(server.getAttribute(objectName, attr.getName())));
            System.exit(0);
          } else if (attr.isWritable()){
            Class<?> clazz = ClassUtils.getClass(attr.getType());
            Object val = null;
            if (clazz.isArray()) {
              val = ConvertUtils.convert(argList.toArray(), clazz);
            } else {
              val = ConvertUtils.convert(argList.get(0), clazz);
            }
            Attribute newAttr = new Attribute(attr.getName(), val);
            server.setAttribute(objectName, newAttr);
            System.exit(0);
          }
        }
      }
      System.out.println("No operation/attribute found with name " + operationName + " and " + argList.size() + " argument(s)");
    } catch (Throwable e) {
      LogRecord rec = new LogRecord(Level.WARNING, "Failed to issue jmxoperation to deployer " + deployerName);
      rec.setThrown(e);
      log.log(rec);
      System.exit(1);
    }
  }
  private JMXConnector resoveConnection(List<String> argList) {
    String first = argList.remove(0);
    JMXConnector conn = null;
    JMXConnector childConn = null;
    if (first.matches("\\d+")) {
      conn = getJMXConnector(Long.parseLong(first));
    }
    if (conn != null) return conn;
    deployerName = first;
    long mypid = sigar.getPid();
    if (mypid <= 0) {
      LogRecord rec = new LogRecord(Level.WARNING, "Failed resolve own pid");
      log.log(rec);
      System.exit(1);
    }
    try {
      long firstPid = findOldDeployerPid(deployerName);
      if (firstPid <= 0) {
        LogRecord rec = new LogRecord(Level.WARNING, "Failed to find pid for deployer " + deployerName);
        log.log(rec);
        System.exit(1);
      }
      conn = getJMXConnector(firstPid);
      if (conn == null) {
        LogRecord rec = new LogRecord(Level.WARNING, "Failed to connect to deployer " + deployerName);
        log.log(rec);
        System.exit(1);
      }
      MBeanServerConnection server = conn.getMBeanServerConnection();
      MainMBean proxy = JMX.newMBeanProxy(server, OBJECT_NAME, MainMBean.class);
      long childPid = proxy.getChildPid(argList.get(0));
      if (childPid > 0) {
        childConn = getJMXConnector(childPid);
        if (childConn == null) {
          LogRecord rec = new LogRecord(Level.WARNING, "Failed to connect to deployer child" + deployerName
              + "::"  + argList.get(0));
          log.log(rec);
          System.exit(1);
        } else {
          argList.remove(0);
          return childConn;
        }
      } else {
        return conn;
      }
    } catch (SigarException | IOException e) {
      log.info("Failed to resolve JMXConnector: " + e.getMessage());
      System.exit(1);
    } finally {
      if (conn != null && childConn != null) {
        try {
          conn.close();
        } catch (IOException e) {
          log.info("Failed to close JMXConnector: " + e.getMessage());
        }
      }
    }
    return null;
  }
}
