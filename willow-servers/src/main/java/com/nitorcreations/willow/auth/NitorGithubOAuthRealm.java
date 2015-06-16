package com.nitorcreations.willow.auth;

import static java.util.Collections.unmodifiableSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.authz.Permission;

import com.google.common.collect.ImmutableMap;

public class NitorGithubOAuthRealm extends GitHubOAuthRealm {

  static final Map<String,? extends Permission> PERMISSIONS = ImmutableMap.of(
      "NitorCreations.willow", Permissions.ADMIN,
      "NitorCreations",        Permissions.MONITOR);

  @Override
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
