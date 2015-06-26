package com.nitorcreations.willow.sshagentauth;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;

public class SSHAgentAuthentication extends AbstractSSHAuthentication {

  private static Logger logger = Logger.getLogger(SSHAgentAuthentication.class.getCanonicalName());

  @SuppressWarnings("unchecked")
  @Override
  public String getSshSignatures(byte[] sign) {
    Connector con = null;
    try {
      ConnectorFactory cf = ConnectorFactory.getDefault();
      con = cf.createConnector();
    } catch (AgentProxyException e) {
      logger.log(Level.SEVERE, "Unable to fetch authorization keys!", e);
    }
    if (con != null) {
      IdentityRepository irepo = new RemoteIdentityRepository(con);
      return getSshSignatures(sign, irepo.getIdentities());
    }
    return "";
  }
}
