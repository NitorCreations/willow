package com.nitorcreations.willow.sshagentauth;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jcraft.jsch.Identity;

public abstract class AbstractSSHAuthentication implements SSHAuthentication {
  private static Logger logger = Logger.getLogger(AbstractSSHAuthentication.class.getCanonicalName());

  public String getSshSignatures(byte[] sign, List<Identity> identities) {
    StringBuilder ret = new StringBuilder();
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
