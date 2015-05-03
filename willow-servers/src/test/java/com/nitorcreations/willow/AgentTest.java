package com.nitorcreations.willow;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.List;

import org.apache.shiro.codec.Hex;
import org.junit.Test;

import com.jcraft.jsch.Identity;
import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;
import com.jcraft.jsch.jce.SignatureDSA;
import com.nitorcreations.willow.auth.AuthorizedKeys;

public class AgentTest {

  @Test
  public void testSignature() throws Exception {
    Connector con = null;
    try {
      ConnectorFactory cf = ConnectorFactory.getDefault();
      con = cf.createConnector();
    } catch (AgentProxyException e) {
      System.out.println(e);
    }
    if (con != null) {
      IdentityRepository irepo = new RemoteIdentityRepository(con);
      for (String run : new String[] { "first", "second", "third" }) {
        for (Identity id : (List<Identity>)irepo.getIdentities()) {
          byte[] sig = id.getSignature("foo".getBytes());
          List<byte[]> sigComponents = AuthorizedKeys.components(sig);
          byte[] key = id.getPublicKeyBlob();
          List<byte[]> keyComponents = AuthorizedKeys.components(key);
          String alg = new String(keyComponents.get(0), StandardCharsets.US_ASCII);
          if ("ssh-dss".equals(alg)) {
              SignatureDSA dsaSig = new SignatureDSA();
              dsaSig.init();
              dsaSig.setPubKey(keyComponents.get(4), keyComponents.get(1), 
                keyComponents.get(2), keyComponents.get(3));
              dsaSig.update("foo".getBytes());
              assertTrue(dsaSig.verify(sig));
          } else {
            Signature signature = Signature.getInstance("SHA1withRSA");
            RSAPublicKeySpec kSpec = new RSAPublicKeySpec(new BigInteger(keyComponents.get(2)), new BigInteger(keyComponents.get(1)));
            signature.initVerify(KeyFactory.getInstance("RSA").generatePublic(kSpec));
            signature.update("foo".getBytes());
            assertTrue(signature.verify(sigComponents.get(1)));
          }
          System.out.println(run + ":" + alg + ":" + id.getName() + ":" + Hex.encodeToString(id.getPublicKeyBlob()));
          System.out.println(run + ":" + alg + ":" + id.getName() + ":" + Hex.encodeToString(sig));
        }
      }
    }
  }
}
