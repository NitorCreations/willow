package com.nitorcreations.willow.sshagentauth;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Signature;
import com.jcraft.jsch.jce.SignatureDSA;
import com.jcraft.jsch.jce.SignatureRSA;

public class SSHUtil {
  private static Logger logger = Logger.getLogger(SSHUtil.class.getCanonicalName());
  private static SSHAuthentication sshAuthentication;
  private static String ENV_SSH_ID = "W_SSH_IDENTITY";
  private final static SecureRandom random = new SecureRandom();
  static {
    String sshId = System.getenv(ENV_SSH_ID);
    if (sshId != null) {
      sshAuthentication = new PrivateKeySSHAuthentication();
      try {
        ((PrivateKeySSHAuthentication)sshAuthentication).addIdentity(sshId);
      } catch (JSchException e) {
        logger.log(Level.WARNING, "Failed to add key - reverting to ssh agent authentication", e);
        sshAuthentication = new SSHAgentAuthentication();
      }
    } else {
      sshAuthentication = new SSHAgentAuthentication();
      byte[] sign = new byte[40];
      random.nextBytes(sign);
      String sigs = sshAuthentication.getSshSignatures(sign);
      if (sigs.isEmpty()) {
        String home = System.getProperty("user.home");
        String sshDir = home + File.separator + ".ssh" + File.separator;
        String[] defaultKeys = new String[] { 
            sshDir + "id_rsa", sshDir + "id_dsa", sshDir + "identity"
        };
        sshAuthentication = new PrivateKeySSHAuthentication();
        for (String nextKey : defaultKeys) {
          try {
            ((PrivateKeySSHAuthentication)sshAuthentication).addIdentity(nextKey);
          } catch (JSchException e) {
            logger.log(Level.FINE, "Failed to add key " + nextKey, e);
          }
        }
      }
    }
  }
  public static boolean verify(byte[] nextSig, String type, List<byte[]> keycomponents, byte[] sign, String keyInfo) {
    String sigtype = new String(components(nextSig).get(0), StandardCharsets.UTF_8);
    if (!sigtype.equals(type)) return false;
    Signature sig = null;
    if ("ssh-dss".equals(type)) {
      SignatureDSA dsaSig = new SignatureDSA();
      try {
        dsaSig.init();
      } catch (Exception e) {
        assert false: "These algorithms should always be available";
      }
      try {
        dsaSig.setPubKey(keycomponents.get(4), keycomponents.get(1), 
          keycomponents.get(2), keycomponents.get(3));
      } catch (Exception e) {
        logger.log(Level.WARNING, "Failed to set public key", e);
        return false;
      }
      sig = dsaSig;
    } else if ("ssh-rsa".equals(type)) {
      SignatureRSA rsaSig = new SignatureRSA();
      try {
        rsaSig.init();
      } catch (Exception e) {
        assert false: "These algorithms should always be available";
      }
      try {
        rsaSig.setPubKey(keycomponents.get(1), keycomponents.get(2));
      } catch (Exception e) {
        logger.log(Level.WARNING, "Failed to set public key", e);
        return false;
      }
      sig = rsaSig;
    } else {
      return false;
    }
    try {
      sig.update(sign);
      try {
        if (sig.verify(nextSig)) {
          logger.fine("Matched key " + keyInfo);
          return true;
        }
      } catch (Exception t) {
        logger.finer("Did not verify with " + keyInfo);
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to verify signature", e);
      return false;
    }
    return false;
  }
  public static List<byte[]> components(byte[] val) {
    List<byte[]> ret = new ArrayList<>();
    int index = 0;
    while (index < val.length) {
      byte[] len = new byte[4];
      System.arraycopy(val, index, len, 0, 4);
      BigInteger lenBi = new BigInteger(len);
      index += 4;
      byte[] next = new byte[lenBi.intValue()];
      System.arraycopy(val, index, next, 0, next.length);
      ret.add(next);
      index += lenBi.intValue();
    }
    return ret;
  }
  public static String getPublicKeyAuthorization(String username) {
    StringBuilder ret = new StringBuilder("PUBLICKEY ");
    String now = Long.toString(System.currentTimeMillis());
    byte[] rnd = new byte[39];
    random.nextBytes(rnd);
    byte[] sign = (username + ":" + now + ":" + printBase64Binary(rnd)).getBytes(StandardCharsets.UTF_8);
    ret.append(printBase64Binary(sign));
    ret.append(sshAuthentication.getSshSignatures(sign));
    return ret.toString();
  }
}
