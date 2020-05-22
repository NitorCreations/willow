package com.nitorcreations.willow.utils;

import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_SUFFIX_DOWNLOAD_IGNORE_MD5;
import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_SUFFIX_DOWNLOAD_MD5;
import static com.nitorcreations.willow.utils.ReplaceTokensInputStream.AT_DELIMITERS;
import static com.nitorcreations.willow.utils.ReplaceTokensInputStream.CURLY_DELIMITERS;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;



public class FileUtil {
  public static final int BUFFER_LEN = 8 * 1024;
  public static final Logger logger = Logger.getLogger(FileUtil.class.getCanonicalName());
  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

  public static String printBase64Binary(byte[] data) {
    return Base64.getEncoder().encodeToString(data);
  }

  public static byte[] parseBase64Binary(String data) {
    return Base64.getDecoder().decode(data.trim());
  }

  public static byte[] parseHexBinary(String hex) {
    if (hex == null || hex.length() == 0) {
      return new byte[0];
    }
    int l = hex.length();
    byte[] data = new byte[l/2];
    for (int i = 0; i < l; i += 2) {
        data[i/2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                             + Character.digit(hex.charAt(i+1), 16));
    }
    return data;
  }

  public static String printHexBinary(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return "";
    }
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
        int v = bytes[j] & 0xFF;
        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static synchronized boolean createDir(File dir) {
    if (dir == null) {
      return false;
    }
    return dir.exists() || dir.mkdirs();
  }

  public static String getFileName(String name) {
    int lastSeparator = Math.max(Math.max(name.lastIndexOf(':'), name.lastIndexOf('/')), name.lastIndexOf('\\'));
    int queryIndex = name.lastIndexOf("?");
    if (queryIndex < 0) {
      queryIndex = name.length();
    }
    return name.substring(lastSeparator + 1, queryIndex);
  }
  public static String getFileNameAndQuery(String name) {
    int lastSeparator = Math.max(Math.max(name.lastIndexOf(':'), name.lastIndexOf('/')), name.lastIndexOf('\\'));
    return name.substring(lastSeparator + 1);
  }
  public static String getFilePath(String name) {
    int lastSeparator = Math.max(Math.max(name.lastIndexOf(':'), name.lastIndexOf('/')), name.lastIndexOf('\\'));
    if (lastSeparator == -1 || lastSeparator >= name.length() -1) return name;
    return name.substring(0, lastSeparator + 1);
  }

  public static long copy(InputStream in, File target) throws IOException {
    return copy(in, target, null);
  }
  public static long copy(InputStream in, File target, DownloadLogger logger) throws IOException {
    try (FileOutputStream out = new FileOutputStream(target)) {
      long lenght = copy(in, out, logger);
      out.getFD().sync();
      return lenght;
    }
  }

  public static long copy(InputStream in, OutputStream out) throws IOException {
    return copy(in, out, null);
  }
  public static long copy(InputStream in, OutputStream out, DownloadLogger logger) throws IOException {
    byte[] buffer = new byte[BUFFER_LEN];
    long count = 0;
    int n = 0;
    while (-1 != (n = in.read(buffer))) {
      out.write(buffer, 0, n);
      count += n;
      if (logger != null) {
        logger.log(count);
      }
    }
    out.flush();
    return count;
  }

  public static long filterStream(InputStream original, File target, Map<String, String> replaceTokens) throws IOException {
    return filterStream(original, target, replaceTokens, null);
  }
  public static long filterStream(InputStream original, File target, Map<String, String> replaceTokens, DownloadLogger logger) throws IOException {
    try (FileOutputStream out = new FileOutputStream(target)) {
      return filterStream(original, out, replaceTokens, logger);
    }
  }

  public static long filterStream(InputStream original, OutputStream out, Map<String, String> replaceTokens) throws IOException {
    return filterStream(original, out, replaceTokens, null);
  }
  public static long filterStream(InputStream original, OutputStream out, Map<String, String> replaceTokens, DownloadLogger logger) throws IOException {
    try (InputStream in = new ReplaceTokensInputStream(new BufferedInputStream(original, BUFFER_LEN), StandardCharsets.UTF_8, replaceTokens, AT_DELIMITERS, CURLY_DELIMITERS)) {
      long length = copyByteByByte(in, out, logger);
      return length;
    }
  }

  public static long copyByteByByte(InputStream in, OutputStream out) throws IOException {
    return copyByteByByte(in, out, null);
  }

  public static long copyByteByByte(InputStream in, OutputStream out, DownloadLogger logger) throws IOException {
    BufferedOutputStream bOut = null;
    if (out instanceof BufferedOutputStream) {
      bOut = (BufferedOutputStream) out;
    } else {
      bOut = new BufferedOutputStream(out, BUFFER_LEN);
    }
    try {
      long i = 0;
      int b;
      while ((b = in.read()) != -1) {
        bOut.write(b);
        i++;
        if (logger != null) {
          logger.log(i);
        }
      }
      return i;
    } catch (IOException e) {
      throw e;
    } finally {
      try {
        bOut.flush();
        if (out instanceof FileOutputStream) {
          ((FileOutputStream) out).getFD().sync();
        }
      } catch (IOException e0) {
        throw e0;
      } finally {
        try {
          if (in != null) {
            in.close();
          }
        } catch (IOException e1) {
          throw e1;
        } finally {
          if (bOut != null) {
            bOut.close();
          }
        }
      }
    }
  }

  public static byte[] getMd5(Properties properties) throws IOException {
    byte[] md5 = null;
    String url = properties.getProperty("");
    if (url == null) {
      url = properties.getProperty("url");
    }
    String urlMd5 = url + ".md5";
    String propMd5 = properties.getProperty(PROPERTY_KEY_SUFFIX_DOWNLOAD_MD5);
    if (propMd5 != null) {
        md5 = parseHexBinary(propMd5);
        if (md5 == null || md5.length < 16) {
          logger.warning("Invalid md5sum: " + propMd5);
          return null;
        }
    } else {
      try {
        md5 = MD5SumInputStream.getMd5FromURL(new URL(urlMd5));
      } catch (IOException e) {
        logger.log(Level.INFO, "No md5 sum available" + urlMd5);
        if (!"true".equalsIgnoreCase(properties.getProperty(PROPERTY_KEY_SUFFIX_DOWNLOAD_IGNORE_MD5))) {
          throw new IOException("Failed to get a valid md5sum for " + url, e);
        }
      }
    }
    return md5;
  }
}
