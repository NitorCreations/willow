package com.nitorcreations.willow.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.shiro.codec.Base64;

import com.google.inject.ConfigurationException;
import com.google.inject.spi.Message;
import com.nitorcreations.willow.utils.ProxyUtils;

public class AuthorizedKeys {
  public static class AuthorizedKey {
    public String type;
    public List<byte[]> keycomponents;
    public String comment;
  }
  private final List<AuthorizedKey> keys = new ArrayList<>();
  
  public static AuthorizedKeys fromUrl(String url) throws ConfigurationException {
    AuthorizedKeys ret = new AuthorizedKeys();
    try (InputStream in = ProxyUtils.getUriInputStream(null, null, url);
      InputStreamReader ir = new InputStreamReader(in);
      BufferedReader read = new BufferedReader(ir)) {
      String line = null;
      while (null != (line = read.readLine())) {
        if (line.isEmpty() || line.trim().startsWith("#")) {
          continue;
        } else {
          String[] elems = line.split("\\s+");
          if (elems.length < 3) continue;
          AuthorizedKey key = new AuthorizedKey();
          key.type = elems[0];
          key.keycomponents = components(Base64.decode(elems[1]));
          key.comment = elems[2];
          ret.addKey(key);
        }
      }
      return ret;
    } catch (IOException | URISyntaxException e) {
      throw new ConfigurationException(Arrays.asList(new Message("Failed to read authorized keys"), new Message(e.getMessage())));
    }
  }
  public List<AuthorizedKey> keys() {
    return keys;
  }
  public void addKey(AuthorizedKey key) {
    keys.add(key);
  }
  public static List<byte[]> components(byte[] val) {
    List<byte[]> ret = new ArrayList<>();
    int index = 0;
    while (index < val.length) {
      byte[] len = new byte[4];
      System.arraycopy(val, index, len, 0, 4);
      BigInteger lenBi = new BigInteger(len);
      index += 4;
      byte[] next = new byte[lenBi.intValue()];
      System.arraycopy(val, index, next, 0, next.length);
      ret.add(next);
      index += lenBi.intValue();
    }
    return ret;
  }

}
 