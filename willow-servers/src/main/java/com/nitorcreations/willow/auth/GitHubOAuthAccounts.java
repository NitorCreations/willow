package com.nitorcreations.willow.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.shiro.authc.SimpleAccount;


@Singleton
@Named
public class GitHubOAuthAccounts {
  private final Map<String, SimpleAccount> accounts = new ConcurrentHashMap<>();
  public GitHubOAuthAccounts() {}
  public void add(SimpleAccount account) {
    Object principal = account.getPrincipals().getPrimaryPrincipal();
    if (principal != null) {
      accounts.put(principal.toString(), account);
    }
  }
  public SimpleAccount get(String principal) {
    return accounts.get(principal);
  }
}
