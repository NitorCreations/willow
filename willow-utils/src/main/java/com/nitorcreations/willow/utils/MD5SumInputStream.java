package com.nitorcreations.willow.utils;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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

  public static byte[] getMd5FromURL(URL url) throws IOException {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
      InputStream in = url.openConnection().getInputStream()) {
      int read=0;
      byte[] buff = new byte[1024 * 4];
      while (-1 < (read = in.read(buff))) {
        out.write(buff, 0, read);
      }
      String md5Str = new String(out.toByteArray(), 0, 32);
      return hexStringToByteArray(md5Str);
    }
  }

  public static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
        + Character.digit(s.charAt(i+1), 16));
    }
    return data;
  }
}
