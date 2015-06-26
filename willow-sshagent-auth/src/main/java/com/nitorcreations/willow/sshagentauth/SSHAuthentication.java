package com.nitorcreations.willow.sshagentauth;


public interface SSHAuthentication {
  String getSshSignatures(byte[] sign);
}
