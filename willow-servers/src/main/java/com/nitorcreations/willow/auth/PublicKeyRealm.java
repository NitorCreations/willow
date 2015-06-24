package com.nitorcreations.willow.auth;

import static com.nitorcreations.willow.sshagentauth.SSHUtil.verify;

import java.util.logging.Logger;

import javax.inject.Singleton;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.SimplePrincipalCollection;

import com.nitorcreations.willow.auth.AuthorizedKeys.AuthorizedKey;

@Singleton
public class PublicKeyRealm implements Realm {
  private AuthorizedKeys authorizedKeys;
  private final Logger log = Logger.getLogger(this.getClass().getName());
  public PublicKeyRealm() {
  }
  public PublicKeyRealm(AuthorizedKeys authorizedKeys) {
    this();
    if (authorizedKeys != null) {
      this.setAuthorizedKeys(authorizedKeys);
    }
  }
  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }
  @Override
  public boolean supports(AuthenticationToken token) {
    return token instanceof PublicKeyAuthenticationToken;
  }
  @Override
  public AuthenticationInfo getAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    PublicKeyAuthenticationToken pkToken;
    if (token instanceof PublicKeyAuthenticationToken) {
      pkToken= (PublicKeyAuthenticationToken)token;
    } else {
      return null;
    }
    boolean found = false;
    for (AuthorizedKey next : authorizedKeys.keys()) {
      for (byte[] nextSig : pkToken.getSignatures()) {
        if (verify(nextSig, next.type, next.keycomponents, pkToken.getSign(), next.comment)) {
          found = true;
          log.fine("Successful auth for " + pkToken.getUsername());
          break;
        }
      }
      if (found) {
        break;
      }
    }
    if (found) {
      SimpleAuthenticationInfo ret = new SimpleAuthenticationInfo();
      ret.setPrincipals(new SimplePrincipalCollection(pkToken.getUsername(), getName()));
      ret.setCredentials(pkToken.getCredentials());
      return ret;
    }
    return null;
  }
  public AuthorizedKeys getAuthorizedKeys() {
    return authorizedKeys;
  }
  public void setAuthorizedKeys(AuthorizedKeys authorizedKeys) {
    this.authorizedKeys = authorizedKeys;
  }
}
