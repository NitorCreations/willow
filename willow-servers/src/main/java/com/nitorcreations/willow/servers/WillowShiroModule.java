package com.nitorcreations.willow.servers;

import javax.servlet.Filter;
import javax.servlet.ServletContext;

import org.apache.shiro.config.Ini;
import org.apache.shiro.guice.web.ShiroWebModule;
import org.apache.shiro.realm.text.IniRealm;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.nitorcreations.willow.auth.AuthorizedKeys;
import com.nitorcreations.willow.auth.PublicKeyAuthenticationFilter;
import com.nitorcreations.willow.auth.PublicKeyRealm;

public class WillowShiroModule extends ShiroWebModule {
  public WillowShiroModule(ServletContext servletContext) {
    super(servletContext);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void configureShiroWeb() {
    doBindRealm();
    addFilterChain("/test/**", ANON);
    addFilterChain("/statistics/**", getDeployerFilter());
    addFilterChain("/properties/**", getDeployerFilter());
    addFilterChain("/search/**", getEndUserFilter());
    addFilterChain("/metrics/**", ANON);
    addFilterChain("/rawterminal/**", getEndUserFilter());
    addFilterChain("/**", getEndUserFilter());
  }

  protected void doBindRealm() {
    try {
      bindRealm().toConstructor(IniRealm.class.getConstructor(Ini.class)).asEagerSingleton();
      bindRealm().toConstructor(PublicKeyRealm.class.getConstructor(AuthorizedKeys.class)).asEagerSingleton();
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
    return AUTHC_BASIC;
  }
  protected Key<? extends Filter> getDeployerFilter() {
    return ANON;
  }
}