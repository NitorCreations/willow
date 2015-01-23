package com.nitorcreations.willow.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5SumInputStream extends FilterInputStream {
  private final MessageDigest digest;

  public MD5SumInputStream(InputStream in) throws NoSuchAlgorithmException {
    super(in);
    digest = MessageDigest.getInstance("MD5");
  }

  @Override
  public int read() throws IOException {
    int ret = super.read();
    if (ret > -1) {
      digest.update((byte) (ret & 0xFF));
    }
    return ret;
  }

  @Override
  public int read(byte[] buff, int off, int len) throws IOException {
    int ret = super.read(buff, off, len);
    if (ret > 0) {
      digest.update(buff, off, ret);
    }
    return ret;
  }

  public byte[] digest() {
    return digest.digest();
  }
}
