package com.nitorcreations.willow.servers;


import javax.servlet.ServletContext;

import org.apache.shiro.config.Ini;
import org.apache.shiro.guice.web.ShiroWebModule;
import org.apache.shiro.realm.text.IniRealm;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;

import com.google.inject.ConfigurationException;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.nitorcreations.willow.auth.GitHubOAuthAuthenticatingFilter;
import com.nitorcreations.willow.auth.GitHubOAuthConfig;
import com.nitorcreations.willow.auth.GitHubOAuthRealm;

public class EndUserShiroModule extends ShiroWebModule {
  public EndUserShiroModule(ServletContext servletContext) {
    super(servletContext);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void configureShiroWeb() {
    Key<? extends AuthenticatingFilter> endUserFilter = getEndUserFilter();
    bindEnduserRealm();
    addFilterChain("/healthcheck/**", ANON);
    addFilterChain("/test/**", ANON);
    addFilterChain("/properties/**", endUserFilter);
    addFilterChain("/search/**", endUserFilter);
    addFilterChain("/metrics/**", endUserFilter);
    addFilterChain("/poll/**", endUserFilter);
    addFilterChain("/logout/**", LOGOUT);
    addFilterChain("/rawterminal/**", endUserFilter);
    addFilterChain("/**", endUserFilter);
  }
  protected void bindEnduserRealm() {
    try {
      if(useGitHubOAuth()) {
        bindRealm().toConstructor(GitHubOAuthRealm.class.getConstructor(GitHubOAuthConfig.class)).asEagerSingleton();
      } else {
        bindRealm().toConstructor(IniRealm.class.getConstructor(Ini.class)).asEagerSingleton();
      }
    } catch (NoSuchMethodException e) {
      addError(e);
    }
  }
  @Provides
  Ini loadShiroIni() {
    return Ini.fromResourcePath(System.getProperty("shiro.ini", "classpath:shiro.ini"));
  }
  @Provides
  GitHubOAuthConfig loadGitHubOAuthConfig() {
    return GitHubOAuthConfig.fromUrl(System.getProperty("github.oauthconf", "classpath:github-oauth.conf"));
  }
  protected Key<? extends AuthenticatingFilter> getEndUserFilter() {
    return useGitHubOAuth()
        ? Key.get(GitHubOAuthAuthenticatingFilter.class)
            : AUTHC_BASIC;
  }
  private boolean useGitHubOAuth() {
    try {
      return !GitHubOAuthConfig.fromUrl(System.getProperty("github.oauthconf", "classpath:github-oauth.conf")).getClientId().isEmpty();
    } catch (ConfigurationException e) {
      return false;
    }
  }
}