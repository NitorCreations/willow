package com.nitorcreations.willow.auth;

import java.util.ArrayList;
import java.util.List;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.HostAuthenticationToken;

public class PublicKeyAuthenticationToken implements AuthenticationToken, HostAuthenticationToken {
  private static final long serialVersionUID = -5835308990612834491L;
  private byte[] sign = new byte[0];
  private final List<byte[]> signatures = new ArrayList<>();
  private String username;
  private String host;

  public PublicKeyAuthenticationToken() {
  }
  public PublicKeyAuthenticationToken(String username, byte[] sign, List<byte[]> signatures, String host) {
    this.username = username;
    this.host = host;
    setSign(sign);
    addSignatures(signatures);
  }
  public PublicKeyAuthenticationToken(String username, byte[] sign, byte[] signature, String host) {
    this.username = username;
    this.host = host;
    setSign(sign);
    addSignature(signature);
  }
  public byte[] getSign() {
    byte[] ret = new byte[sign.length];
    System.arraycopy(sign, 0, ret, 0, sign.length);
    return ret;
  }
  public void setSign(byte[] sign) {
    if (sign != null) {
      this.sign = new byte[sign.length];
      System.arraycopy(sign, 0, this.sign, 0, sign.length);
    } else {
      this.sign = new byte[0];
    }
  }
  public List<byte[]> getSignAndSignatures() {
    ArrayList<byte[]> ret = new ArrayList<>();
    ret.add(sign);
    ret.addAll(signatures);
    return ret;
  }
  public List<byte[]> getSignatures() {
    ArrayList<byte[]> ret = new ArrayList<>();
    ret.addAll(signatures);
    return ret;
  }
  public void addSignature(byte[] signature) {
    if (signature != null) {
      signatures.add(signature);
    }
  }
  public void setSignatures(List<byte[]> signatures) {
    this.signatures.clear();
    if (signatures != null) {
      this.signatures.addAll(signatures);
    }
  }
  public void addSignatures(List<byte[]> signatures) {
    if (signatures != null) {
      this.signatures.addAll(signatures);
    }
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
  public String getUsername() {
    return username;
  }
  @Override
  public String getHost() {
    return host;
  }
  public void setHost(String host) {
    this.host = host;
  }
}