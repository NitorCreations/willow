package com.nitorcreations.willow.servers;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.nitorcreations.willow.auth.*;
import org.apache.shiro.config.Ini;
import org.apache.shiro.guice.web.ShiroWebModule;
import org.apache.shiro.realm.text.IniRealm;

import javax.servlet.Filter;
import javax.servlet.ServletContext;

public class WillowShiroModule extends ShiroWebModule {
  public WillowShiroModule(ServletContext servletContext) {
    super(servletContext);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void configureShiroWeb() {
    Key<? extends Filter> endUserFilter = getEndUserFilter();

    doBindRealm();
    addFilterChain("/test/**", ANON);
    addFilterChain("/statistics/**", getDeployerFilter());
    addFilterChain("/properties/**", getDeployerFilter());
    addFilterChain("/search/**", endUserFilter);
    addFilterChain("/metrics/**", ANON);
    addFilterChain("/poll/**", ANON);
    addFilterChain("/rawterminal/**", endUserFilter);
    addFilterChain("/**", endUserFilter);
  }

  protected void doBindRealm() {
    try {
      bindRealm().toConstructor(PublicKeyRealm.class.getConstructor(AuthorizedKeys.class)).asEagerSingleton();
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

  protected Key<? extends Filter> getEndUserFilter() {
    return useGitHubOAuth()
            ? Key.get(GitHubOAuthAuthenticatingFilter.class)
            : AUTHC_BASIC;
  }
  protected Key<? extends Filter> getDeployerFilter() {
    return Key.get(PublicKeyAuthenticationFilter.class);
  }
  private boolean useGitHubOAuth() {
    return getClass().getResource("/github-oauth.properties") != null;
  }
}