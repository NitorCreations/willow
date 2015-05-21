package com.nitorcreations.willow.utils;

import com.jcraft.jsch.Identity;
import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

public class SSHAgentAuthorizationUtil {

  private static Logger logger = Logger.getLogger(SSHAgentAuthorizationUtil.class.getCanonicalName());

  @SuppressWarnings("unchecked")
  public static String getSshAgentAuthorization(String username) {
    StringBuilder ret = new StringBuilder("PUBLICKEY ");
    String now = "" + System.currentTimeMillis();
    Connector con = null;
    try {
      ConnectorFactory cf = ConnectorFactory.getDefault();
      con = cf.createConnector();
    } catch (AgentProxyException e) {
      logger.log(Level.SEVERE, "Unable to fetch authorization keys!", e);
    }
    byte[] sign = (username + ":" + now).getBytes(StandardCharsets.UTF_8);
    ret.append(printBase64Binary(sign));
    if (con != null) {
      IdentityRepository irepo = new RemoteIdentityRepository(con);
      for (Identity id : (List<Identity>)irepo.getIdentities()) {
        try {
          byte[] sig = id.getSignature(sign);
          ret.append(" ").append(printBase64Binary(sig));
        } catch (Throwable t) {
          logger.log(Level.FINE, "Failed to add signature: " + t.getMessage());
        }
      }
    }
    return ret.toString();
  }
}
