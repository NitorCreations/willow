package com.nitorcreations.willow.auth;

import java.util.ArrayList;
import java.util.List;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.HostAuthenticationToken;

public class PublicKeyAuthenticationToken implements AuthenticationToken, HostAuthenticationToken {
  private static final long serialVersionUID = -5835308990612834491L;
  private byte[] sign;
  private final List<byte[]> signatures = new ArrayList<>();
  private String username;
  private String host;

  public PublicKeyAuthenticationToken() {
  }
  public PublicKeyAuthenticationToken(String username, byte[] sign, List<byte[]> signatures, String host) {
    this.username = username;
    this.sign = sign;
    this.host = host;
    addSignatures(signatures);
  }
  public PublicKeyAuthenticationToken(String username, byte[] sign, byte[] signature, String host) {
    this.username = username;
    this.sign = sign;
    this.host = host;
    addSignature(signature);
  }
  public void setSign(byte[] sign) {
    this.sign = sign;
  }
  public void addSignature(byte[] signature) {
    if (signature != null)
      signatures.add(signature);
  }
  public void setSignatures(List<byte[]> signatures) {
    signatures.clear();
    signatures.addAll(signatures);
  }
  public void addSignatures(List<byte[]> signatures) {
    if (signatures == null) {
      signatures = new ArrayList<>();
    }
    signatures.addAll(signatures);
  }
  public void setUsername(String username) {
    this.username = username;
  }
  
  @Override
  public Object getPrincipal() {
    return getUsername();
  }
  @Override
  public Object getCredentials() {
    return getSignAndSignatures();
  }
  public List<byte[]> getSignAndSignatures() {
    ArrayList<byte[]> ret = new ArrayList<>();
    ret.add(sign);
    ret.addAll(signatures);
    return ret;
  }
  public String getUsername() {
    return username;
  }
  public byte[] getSign() {
    return sign;
  }
  public List<byte[]> getSignatures() {
    return signatures;
  }
  @Override
  public String getHost() {
    return host;
  }
  public void setHost(String host) {
    this.host = host;
  }
}
