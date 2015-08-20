package com.nitorcreations.willow.deployer.statistics;

import java.net.InetAddress;
import java.util.Properties;
import java.util.logging.Logger;

import javax.inject.Named;

import com.nitorcreations.willow.messages.HostInfoMessage;
import com.nitorcreations.willow.utils.HostUtil;

@Named("hostinfo")
public class HostInfoSender extends AbstractStatisticsSender {

  private static String PROPERTY_KEY_SSH_USERNAME = "ssh_username";
  private static String PROPERTY_KEY_INTERVAL = "interval";

  private Logger logger = Logger.getLogger(getClass().getName());
  private long interval = 30000;
  private String username;

  @Override
  public void execute() {
    HostInfoMessage him = new HostInfoMessage();
    him.username = username;
    InetAddress privateAddress = HostUtil.getPrivateIpAddress();
    if (privateAddress != null) {
      him.privateIpAddress = privateAddress.getHostAddress();
      him.privateHostname = privateAddress.getCanonicalHostName();
    }
    InetAddress publicAddress = HostUtil.getPublicIpAddress();
    if (publicAddress != null) {
      him.publicIpAddress = publicAddress.getHostAddress();
      him.publicHostname = publicAddress.getCanonicalHostName();
    }
    logger.finest("Sending HostInfoMessage");
    transmitter.queue(him);
    try {
      Thread.sleep(interval);
    } catch (InterruptedException e1) {
      logger.finest("HostInfoSender interrupted");
    }
  }

  @Override
  public void setProperties(Properties properties) {
    interval = Long.parseLong(properties.getProperty(PROPERTY_KEY_INTERVAL, Long.toString(interval)));
    username = properties.getProperty(PROPERTY_KEY_SSH_USERNAME, System.getProperty("user.name"));
  }
}
