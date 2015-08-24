package com.nitorcreations.willow.auth;

import javax.inject.Inject;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.json.JSONException;
import org.json.JSONObject;

import mx.com.inftel.shiro.oauth2.OAuth2AuthenticationToken;

public class GitHubOAuthRealm extends AuthorizingRealm {

  @Inject
  private GitHubOAuthAccounts accounts;

  @Override
  public String getName() {
    return getClass().getName();
  }

  @Override
  public boolean supports(AuthenticationToken authenticationToken) {
    return OAuth2AuthenticationToken.class.isInstance(authenticationToken);
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    return accounts.get(principals.getPrimaryPrincipal().toString());
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    try {
      return accounts.get(((JSONObject)token.getPrincipal()).getString("login"));
    } catch (JSONException e) {
      throw new AuthenticationException("Failed to get user info", e);
    }
  }
}
