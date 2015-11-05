package com.nitorcreations.willow.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class MD5SumInputStream extends DigestInputStream {
  public MD5SumInputStream(final InputStream stream) {
    super(stream, getMD5Digest());
  }
  public static MessageDigest getMD5Digest() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("MD5 message digest not available", e);
    }
  }
  public byte[] digest() {
    return getMessageDigest().digest();
  }
  @SuppressFBWarnings(value={"RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"},
      justification="null check in check-with-resources magic bytecode")
  public static byte[] getMd5FromURL(URL url) throws IOException {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = url.openConnection().getInputStream()) {
      int read=0;
      byte[] buff = new byte[1024 * 4];
      while (-1 < (read = in.read(buff))) {
        out.write(buff, 0, read);
      }
      String md5Str = new String(out.toByteArray(), 0, 32, StandardCharsets.UTF_8);
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
