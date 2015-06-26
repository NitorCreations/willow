package com.nitorcreations.willow.sshagentauth;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.jcraft.jsch.Identity;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

public class PrivateKeySSHAuthentication extends AbstractSSHAuthentication {
  private List<Identity> identities = new ArrayList<>();
  private static Logger logger = Logger.getLogger(PrivateKeySSHAuthentication.class.getCanonicalName());

  public void addIdentity(String identityfile) throws JSchException {
    addIdentity(identityfile, null);
  }
  public void addIdentity(String identityfile, String passphrase) throws JSchException {
    JSch jsch = new JSch();
    jsch.addIdentity(identityfile, passphrase);
    identities.addAll(jsch.getIdentityRepository().getIdentities());
    logger.info("Added " + identityfile);
  }
  @Override
  public String getSshSignatures(byte[] sign) {
    return getSshSignatures(sign, identities);
  }


}
