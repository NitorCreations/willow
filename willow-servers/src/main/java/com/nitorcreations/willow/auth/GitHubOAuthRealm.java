package com.nitorcreations.willow.auth;

import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.realm.Realm;
import org.json.JSONException;
import org.json.JSONObject;

import mx.com.inftel.shiro.oauth2.OAuth2AuthenticationToken;

public abstract class GitHubOAuthRealm implements Realm {

  @Override
  public String getName() {
    return getClass().getName();
  }

  @Override
  public boolean supports(AuthenticationToken authenticationToken) {
    return OAuth2AuthenticationToken.class.isInstance(authenticationToken);
  }

  @Override
  public AuthenticationInfo getAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
    JSONObject user = (JSONObject) authenticationToken.getPrincipal();
    try {
      String userId = user.getString("login");
      Set<String> memberOf = JSONTool.toStringSet(user.getJSONArray("member_of"));
      return new SimpleAccount(userId, null, getName(), memberOf, memberShipsToPermissions(memberOf));
    } catch (JSONException e) {
      throw new AuthenticationException(e);
    }
  }

  protected abstract Set<Permission> memberShipsToPermissions(Set<String> organizations);


}
