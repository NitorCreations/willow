package com.nitorcreations.willow.deployer.statistics;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.inject.Named;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.beanutils.ConvertUtils;

import com.nitorcreations.willow.messages.HashMessage;
import com.nitorcreations.willow.utils.MergeableProperties;

@Named("customjmx")
public class CustomJMXStatsSender extends AbstractJMXStatisticsSender {
  private long nextJmx;
  private long interval;
  private Map<String, JMXReader> props = new LinkedHashMap<>();
  private Map<Integer, OperationDesriptor> opDescs = new HashMap<>();
  private interface JMXReader {
    Object read(MBeanServerConnection server) throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, IOException, IntrospectionException, ClassNotFoundException;
  }
  private static class OperationDesriptor {
    public Object[] params;
    public String[] signature;
  }
  @Override
  public void setProperties(Properties properties) {
    super.setProperties(properties);
    MergeableProperties tmp = new MergeableProperties();
    tmp.putAll(properties);
    List<String> lst = tmp.getArrayProperty("property");
    for (int i=0; i<lst.size(); i++) {
      String reportname = lst.get(i);
      try {
        final ObjectName objectName = new ObjectName(properties.getProperty("property[" + i + "].objectname"));
        final String attribute = properties.getProperty("property[" + i + "].attribute");
        if (attribute != null) {
          props.put(reportname, new JMXReader() {
            @Override
            public Object read(MBeanServerConnection server) throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, IOException {
              return server.getAttribute(objectName, attribute);
            }
          });
        } else {
          final String operationName = properties.getProperty("property[" + i + "].method");
          final List<String> argList = tmp.getArrayProperty("property[" + i + "].argument");
          final int index = i;
          if (operationName != null) {
            props.put(reportname, new JMXReader() {
              @Override
              public Object read(MBeanServerConnection server) throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, IOException, IntrospectionException, ClassNotFoundException {
                OperationDesriptor desc = getOperationDescriptor(server, objectName, operationName, argList, index);
                if (desc != null) {
                  return server.invoke(objectName, operationName, desc.params, desc.signature);
                } else {
                  return null;
                }
              }
            });
          }
        }
      } catch (MalformedObjectNameException e) {
      }
    }
    interval = Long.parseLong(properties.getProperty("interval", "5000"));
    nextJmx = System.currentTimeMillis() + interval;
  }
  private OperationDesriptor getOperationDescriptor(MBeanServerConnection server, ObjectName objectName, String operationName, List<String> argList, int i) throws InstanceNotFoundException, ReflectionException, IOException, IntrospectionException, ClassNotFoundException {
    OperationDesriptor desc = opDescs.get(i);
    if (desc != null) return desc;
    desc = new OperationDesriptor();
    MBeanInfo mBeanInfo = server.getMBeanInfo(objectName);
    MBeanOperationInfo[] operations = mBeanInfo.getOperations();
    for (MBeanOperationInfo operation : operations) {
      if (operation.getName().equals(operationName)) {
          MBeanParameterInfo[] parameters = operation.getSignature();
          desc.signature = new String[parameters.length];
          for (int j = 0; j < desc.signature.length; j++) {
            desc.signature[j] = parameters[j].getType();
          }
          desc.params = argList.toArray(new Object[argList.size()]);
          if (desc.params.length != desc.signature.length) {
            continue;
          }
          for (int j = 0; j < desc.signature.length; j++) {
            desc.params[j] = ConvertUtils.convert(desc.params[j], Class.forName(desc.signature[j]));
          }
          opDescs.put(i, desc);
          return desc;
      }
    }
    return null;
  }
  @Override
  public void execute() {
    MBeanServerConnection server = getMBeanServerConnection();
    long now = System.currentTimeMillis();
    if (server != null) {
      if (now > nextJmx) {
        try {
          Map<String, Object> map = new LinkedHashMap<>();
          for (Entry<String, JMXReader> next : props.entrySet()) {
            Object val = next.getValue().read(server);
            if (val != null) {
              map.put(next.getKey(), val.toString());
            }
          }
          if (!map.isEmpty()) {
            HashMessage msg = HashMessage.create(map);
            msg.addTags("category_customjmx_" + getChildName());
            transmitter.queue(msg);
          }
        } catch (IOException | ReflectionException | AttributeNotFoundException
          | InstanceNotFoundException | IntrospectionException | MBeanException
          | ClassNotFoundException e) {
          logger.log(Level.WARNING, "Failed to get JMX statistics", e);
        }
        nextJmx = nextJmx + interval;
      }
    }
    try {
      TimeUnit.MILLISECONDS.sleep(nextJmx - now);
    } catch (InterruptedException e) {
      logger.info("Process statistics interrupted");
      return;
    }
  }
}
