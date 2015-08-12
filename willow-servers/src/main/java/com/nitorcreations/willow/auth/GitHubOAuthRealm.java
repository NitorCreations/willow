package com.nitorcreations.willow.auth;

import static java.util.Collections.unmodifiableSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.realm.Realm;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.ImmutableMap;

import mx.com.inftel.shiro.oauth2.OAuth2AuthenticationToken;

public abstract class GitHubOAuthRealm implements Realm {
  private final Map<String,? extends Permission> PERMISSIONS;

  public GitHubOAuthRealm(GitHubOAuthConfig config) {
    PERMISSIONS = ImmutableMap.of(
        config.getOrganization() + "." + config.getAdminteam(), Permissions.ADMIN,
        config.getOrganization(), Permissions.MONITOR);
  }
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
  protected Set<Permission> memberShipsToPermissions(Set<String> organizations) {
    Set<Permission> permissions = new HashSet<>();
    for(String team : organizations) {
      Permission permission = PERMISSIONS.get(team);
      if(permission != null) {
        permissions.add(permission);
      }
    }
    return unmodifiableSet(permissions);
  }
}
