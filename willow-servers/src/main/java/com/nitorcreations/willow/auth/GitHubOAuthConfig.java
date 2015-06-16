package com.nitorcreations.willow.auth;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;

import com.google.inject.ConfigurationException;
import com.google.inject.spi.Message;

public class GitHubOAuthConfig {
  private final String redirectUri;
  private final String clientId;
  private final String clientSecret;
  public GitHubOAuthConfig(String redirectUri, String clientId, String clientSecret) {
    this.redirectUri = redirectUri;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }
  public static GitHubOAuthConfig fromUrl(String url) throws ConfigurationException {
    try (InputStream in = new URL(url).openStream()){
      Properties config = new Properties();
      config.load(in);
      String redirectUri = config.getProperty("redirect_uri", "");
      String clientId = config.getProperty("client_id", "");
      String clientSecret = config.getProperty("client_secret", "");
      return new GitHubOAuthConfig(redirectUri, clientId, clientSecret);
    } catch (IOException e) {
      throw new ConfigurationException(Arrays.asList(new Message("Failed to read GitHub OAuth config"), new Message(e.getMessage())));
    }
  }
  public String getRedirectUri() {
    return redirectUri;
  }
  public String getClientId() {
    return clientId;
  }
  public String getClientSecret() {
    return clientSecret;
  }

}
