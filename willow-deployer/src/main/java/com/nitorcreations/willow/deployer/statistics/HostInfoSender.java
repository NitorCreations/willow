package com.nitorcreations.willow.deployer.statistics;

import java.net.InetAddress;
import java.util.Properties;
import java.util.logging.Logger;

import javax.inject.Named;

import com.nitorcreations.willow.messages.HostInfoMessage;
import com.nitorcreations.willow.utils.HostUtil;

@Named("hostinfo")
public class HostInfoSender extends AbstractStatisticsSender {

  private Logger logger = Logger.getLogger(getClass().getName());

  @Override
  public void execute() {
    HostInfoMessage him = new HostInfoMessage();
    InetAddress privateAddress = HostUtil.getPrivateIpAddress();
    him.privateIpAddress = privateAddress.getHostAddress();
    him.privateHostname = privateAddress.getCanonicalHostName();
    InetAddress publicAddress = HostUtil.getPublicIpAddress();
    him.publicIpAddress = publicAddress.getHostAddress();
    him.publicHostname = publicAddress.getCanonicalHostName();

    logger.finest("Sending HostInfoMessage");
    transmitter.queue(him);
    try {
      Thread.sleep(30000);
    } catch (InterruptedException e1) {
      logger.finest("HostInfoSender interrupted");
    }
  }

  @Override
  public void setProperties(Properties properties) {

  }
}
