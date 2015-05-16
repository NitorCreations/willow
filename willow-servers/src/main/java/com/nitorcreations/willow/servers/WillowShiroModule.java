package com.nitorcreations.willow.servers;


import com.google.inject.Key;
import com.google.inject.Provides;
import com.nitorcreations.willow.auth.*;

import org.apache.shiro.config.Ini;
import org.apache.shiro.guice.web.ShiroWebModule;
import org.apache.shiro.realm.text.IniRealm;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;

import javax.servlet.ServletContext;

public class WillowShiroModule extends ShiroWebModule {
  public WillowShiroModule(ServletContext servletContext) {
    super(servletContext);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void configureShiroWeb() {
    Key<? extends AuthenticatingFilter> endUserFilter = getEndUserFilter();
    Key<? extends AuthenticatingFilter> deployerFilter = getDeployerFilter(); 
    bindDeployerRealm();
    bindEnduserRealm();
    addFilterChain("/test/**", ANON);
    addFilterChain("/statistics/**", deployerFilter);
    addFilterChain("/properties/**", endUserFilter);
    addFilterChain("/launchproperties/**", deployerFilter);
    addFilterChain("/search/**", endUserFilter);
    addFilterChain("/metrics/**", endUserFilter);
    addFilterChain("/poll/**", endUserFilter);
    addFilterChain("/logout/**", LOGOUT);
    addFilterChain("/rawterminal/**", endUserFilter);
    
    addFilterChain("/**", endUserFilter);
  }
  protected void bindDeployerRealm() {
    try {
      bindRealm().toConstructor(PublicKeyRealm.class.getConstructor(AuthorizedKeys.class)).asEagerSingleton();
    } catch (NoSuchMethodException e) {
      addError(e);
    }    
  }
  protected void bindEnduserRealm() {
    try {
      if(useGitHubOAuth()) {
        bindRealm().to(NitorGithubOAuthRealm.class).asEagerSingleton();
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
  AuthorizedKeys loadAuthorizedKeys() {
    return AuthorizedKeys.fromUrl(System.getProperty("authorized.keys", "classpath:authorized_keys"));
  }

  protected Key<? extends AuthenticatingFilter> getEndUserFilter() {
    return useGitHubOAuth()
            ? Key.get(GitHubOAuthAuthenticatingFilter.class)
            : AUTHC_BASIC;
  }
  protected Key<? extends AuthenticatingFilter> getDeployerFilter() {
    return Key.get(PublicKeyAuthenticationFilter.class);
  }
  private boolean useGitHubOAuth() {
    return getClass().getResource("/github-oauth.properties") != null;
  }

}