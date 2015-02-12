package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.ENV_DEPLOYER_NAME;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.apache.commons.beanutils.ConvertUtils;
import org.hyperic.sigar.ProcTime;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.ptql.ProcessQuery;
import org.hyperic.sigar.ptql.ProcessQueryFactory;

public class JMXOperation extends DeployerControl {
  public static void main(String[] args) {
    new JMXOperation().doMain(args);
  }

  public void doMain(String[] args) {
    final int BASE_ARGS = 3;
    if (args.length < BASE_ARGS) {
      usage("Usage: JMXOperation deployerName objectName operationName [arguments...]");
    }
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
        ObjectName objectName = new ObjectName(args[1]);
        String operationName = args[2];
        MBeanServerConnection server = getMBeanServerConnection(firstPid);
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
              Object[] params = Arrays.copyOfRange(args, BASE_ARGS, args.length, Object[].class);
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
        System.out.println("No operation found with name " + operationName + " and " + (args.length - BASE_ARGS) + " argument(s)");
      } else {
        System.out.println("No deployer with role " + deployerName + " running");
      }
    } catch (Throwable e) {
      LogRecord rec = new LogRecord(Level.WARNING, "Failed to connect to deployer " + deployerName);
      log.log(rec);
    }
  }
}
