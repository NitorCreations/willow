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
  private final String organization;
  private final String adminteam;
  
  public GitHubOAuthConfig(String redirectUri, String clientId, String clientSecret, String organization, String adminteam) {
    this.redirectUri = redirectUri;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.organization = organization;
    this.adminteam = adminteam;
  }
  public static GitHubOAuthConfig fromUrl(String url) throws ConfigurationException {
    try (InputStream in = new URL(url).openStream()){
      Properties config = new Properties();
      config.load(in);
      String redirectUri = config.getProperty("redirect_uri", "");
      String clientId = config.getProperty("client_id", "");
      String clientSecret = config.getProperty("client_secret", "");
      String organization = config.getProperty("organization", "");
      String adminteam = config.getProperty("adminteam", "");
      
      return new GitHubOAuthConfig(redirectUri, clientId, clientSecret, organization, adminteam);
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
  public String getOrganization() {
    return organization;
  }
  public String getAdminteam() {
    return adminteam;
  }

}
