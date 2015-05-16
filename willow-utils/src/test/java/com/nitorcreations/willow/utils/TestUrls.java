package com.nitorcreations.willow.utils;

import static javax.xml.bind.DatatypeConverter.printHexBinary;
import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.net.URLConnection;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.nitorcreations.willow.protocols.Register;

public class TestUrls {
  @BeforeClass
  public static void register() {
    Register.doIt();
  }
  // SSH tests only work for me -> ignore them by default
  @Test
  @Ignore
  public void testSftpRelative() throws Exception {
    URL test = new URL("sftp://localhost/~/tmp/install_lastpass.sh");
    URLConnection conn = test.openConnection();
    try (MD5SumInputStream in = new MD5SumInputStream(conn.getInputStream())) {
      byte[] buff = new byte[1024 * 4];
      while (in.read(buff) > -1) {}
      assertEquals("6ac67b1004aa249b891bfb346138d658".toUpperCase(), printHexBinary(in.digest()));
    }
  }
  @Test
  @Ignore
  public void testSftpAbsolute() throws Exception {
    URL test = new URL("sftp://localhost/home/pasi/tmp/install_lastpass.sh");
    URLConnection conn = test.openConnection();
    try (MD5SumInputStream in = new MD5SumInputStream(conn.getInputStream())) {
      byte[] buff = new byte[1024 * 4];
      while (in.read(buff) > -1) {}
      assertEquals("6ac67b1004aa249b891bfb346138d658".toUpperCase(), printHexBinary(in.digest()));
    }
  }
  @Test
  @Ignore
  public void testSshAbsolute() throws Exception {
    URL test = new URL("ssh://localhost/home/pasi/tmp/install_lastpass.sh");
    URLConnection conn = test.openConnection();
    try (MD5SumInputStream in = new MD5SumInputStream(conn.getInputStream())) {
      byte[] buff = new byte[1024 * 4];
      while (in.read(buff) > -1) {}
      assertEquals("6ac67b1004aa249b891bfb346138d658".toUpperCase(), printHexBinary(in.digest()));
    }
  }
  @Test
  public void testClasspath() throws Exception {
    URL test = new URL("classpath:test-classpath.txt");
    URLConnection conn = test.openConnection();
    try (MD5SumInputStream in = new MD5SumInputStream(conn.getInputStream())) {
      byte[] buff = new byte[1024 * 4];
      while (in.read(buff) > -1) {}
      assertEquals("14a7f05778753dec84782c623292a5f2".toUpperCase(), printHexBinary(in.digest()));
    }
  }
}
