package com.nitorcreations.willow.servers;


import javax.servlet.ServletContext;

import org.apache.shiro.guice.web.ShiroWebModule;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.nitorcreations.willow.auth.AuthorizedKeys;
import com.nitorcreations.willow.auth.PublicKeyAuthenticationFilter;
import com.nitorcreations.willow.auth.PublicKeyRealm;

public class DeployerShiroModule extends ShiroWebModule {
  public DeployerShiroModule(ServletContext servletContext) {
    super(servletContext);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void configureShiroWeb() {
    Key<? extends AuthenticatingFilter> deployerFilter = getDeployerFilter();
    bindDeployerRealm();
    addFilterChain("/metrics-internal/**", deployerFilter);
    addFilterChain("/statistics/**", deployerFilter);
    addFilterChain("/launchproperties/**", deployerFilter);
    addFilterChain("/poll-internal/**", deployerFilter);
    addFilterChain("/logout/**", LOGOUT);
  }
  protected void bindDeployerRealm() {
    try {
      bindRealm().toConstructor(PublicKeyRealm.class.getConstructor(AuthorizedKeys.class)).asEagerSingleton();
    } catch (NoSuchMethodException e) {
      addError(e);
    }
  }
  @Provides
  AuthorizedKeys loadAuthorizedKeys() {
    return AuthorizedKeys.fromUrl(System.getProperty("authorized.keys", "classpath:authorized_keys"));
  }
  protected Key<? extends AuthenticatingFilter> getDeployerFilter() {
    return Key.get(PublicKeyAuthenticationFilter.class);
  }
}