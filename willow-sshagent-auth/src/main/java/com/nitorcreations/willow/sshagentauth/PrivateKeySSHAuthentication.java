package com.nitorcreations.willow.sshagentauth;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jcraft.jsch.Identity;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

public class PrivateKeySSHAuthentication implements SSHAuthentication {
  private List<Identity> identities = new ArrayList<>();
  private static Logger logger = Logger.getLogger(PrivateKeySSHAuthentication.class.getCanonicalName());

  public void addIdentity(String identityfile) throws JSchException {
    addIdentity(identityfile, null);
  }
  public void addIdentity(String identityfile, String passphrase) throws JSchException {
    JSch jsch = new JSch();
    jsch.addIdentity(identityfile, passphrase);
    identities.addAll(jsch.getIdentityRepository().getIdentities());
  }

  @Override
  public String getSshAgentAuthorization(String username) {
    StringBuilder ret = new StringBuilder("PUBLICKEY ");
    String now = Long.toString(System.currentTimeMillis());
    byte[] sign = (username + ":" + now).getBytes(StandardCharsets.UTF_8);
    ret.append(printBase64Binary(sign));
    for (Identity id : identities) {
      try {
        byte[] sig = id.getSignature(sign);
        if (sig != null) {
          ret.append(" ").append(printBase64Binary(sig));
        }
      } catch (Exception t) {
        logger.log(Level.FINE, "Failed to add signature: " + t.getMessage());
      }
    }
    return ret.toString();
  }

}
