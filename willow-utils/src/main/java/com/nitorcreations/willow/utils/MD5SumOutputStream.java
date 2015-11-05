package com.nitorcreations.willow.utils;

import java.io.OutputStream;
import java.security.DigestOutputStream;

public class MD5SumOutputStream extends DigestOutputStream {

  public MD5SumOutputStream(OutputStream stream) {
    super(stream, MD5SumInputStream.getMD5Digest());
  }
  public byte[] digest() {
    return getMessageDigest().digest();
  }

}
