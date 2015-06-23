package com.nitorcreations.willow.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.codec.Base64;

import com.google.inject.ConfigurationException;
import com.google.inject.spi.Message;
import com.nitorcreations.willow.sshagentauth.SSHAgentAuthorizationUtil;
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
        InputStreamReader ir = new InputStreamReader(in, StandardCharsets.UTF_8);
        BufferedReader read = new BufferedReader(ir)) {
      String line = null;
      while (null != (line = read.readLine())) {
        if (StringUtils.isBlank(line) || line.trim().startsWith("#")) {
          continue;
        } else {
          String[] elems = line.split("\\s+");
          if (elems.length < 3) {
            continue;
          }
          AuthorizedKey key = new AuthorizedKey();
          key.type = elems[0];
          key.keycomponents = SSHAgentAuthorizationUtil.components(Base64.decode(elems[1]));
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

}
