package com.nitorcreations.willow.sshagentauth;
import static com.nitorcreations.willow.sshagentauth.SSHUtil.verify;
import static javax.xml.bind.DatatypeConverter.parseBase64Binary;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import com.jcraft.jsch.JSchException;

public class PrivateKeySSHAuthenticationTest {
  @Test
  public void testPrivateKey() throws JSchException {
    String pubKey = "AAAAB3NzaC1yc2EAAAADAQABAAABAQDBUn2Q/U9uzNLIIAhsXKPGiWhbSvss0lVZDHBPe4TxeTGOKg7Z1iLzKgBxCbRIYIBSeojyyzo3yBxumGZw99HGo9vl9v8p3Qh3d1wvCWCo9mfcKvjCIkNKpXmIJHXM2vgOSt+Ara/8aQTTAbCQ3v6lebqLkD/WYyrDkpNpOJbjXQ6VrjLxh9TG/MIXFJ7qUeLnxUUQ2oZKyq5hrlQ/6uJUdCDry4nrIie2p2xLxJOVDdCg/8QRDVd4t8hgS99TdYhJro/AOIc1PB5uRtSTuQVTMhu/vroccbXFEpnoYPnxOePlRIaeSrvJvDVdCI06zbES6TJAEl9F3iP9vhiJejVN";
    List<byte[]> components = SSHUtil.components(parseBase64Binary(pubKey));
    PrivateKeySSHAuthentication test = new PrivateKeySSHAuthentication();
    test.addIdentity("src/test/resources/id_rsa");
    byte[] sign = "kwiidswuywslsgjslghs".getBytes(StandardCharsets.UTF_8);
    String authorizationHeader = test.getSshSignatures(sign);
    assertTrue(verify(parseBase64Binary(authorizationHeader), "ssh-rsa", components, sign, "pasi@venom"));
  }
}
