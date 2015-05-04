package com.nitorcreations.willow.auth;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Singleton;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.SimplePrincipalCollection;

import com.jcraft.jsch.Signature;
import com.jcraft.jsch.jce.SignatureDSA;
import com.jcraft.jsch.jce.SignatureRSA;
import com.nitorcreations.willow.auth.AuthorizedKeys.AuthorizedKey;


@Singleton
public class PublicKeyRealm implements Realm {
  private AuthorizedKeys authorizedKeys;
  private final Logger log = Logger.getLogger(this.getClass().getName());
  public PublicKeyRealm() {
  }
  public PublicKeyRealm(AuthorizedKeys authorizedKeys) {
    this();
    if (authorizedKeys != null)
      this.setAuthorizedKeys(authorizedKeys);
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
    PublicKeyAuthenticationToken pkToken = (PublicKeyAuthenticationToken)token;
    boolean found = false;
    for (AuthorizedKey next : authorizedKeys.keys()) {
      for (byte[] nextSig : pkToken.getSignatures()) {
        String type = new String(AuthorizedKeys.components(nextSig).get(0));
        if (!type.equals(next.type)) continue;
        Signature sig = null;
        if ("ssh-dss".equals(next.type)) {
          SignatureDSA dsaSig = new SignatureDSA();
          try {
            dsaSig.init();
          } catch (Exception e) {
            assert false: "These algorithms should always be available";
          }
          try {
            dsaSig.setPubKey(next.keycomponents.get(4), next.keycomponents.get(1), 
              next.keycomponents.get(2), next.keycomponents.get(3));
          } catch (Exception e) {
            log.log(Level.WARNING, "Failed to set public key", e);
            continue;
          }
          sig = dsaSig;
        } else if ("ssh-rsa".equals(next.type)) {
          SignatureRSA rsaSig = new SignatureRSA();
          try {
            rsaSig.init();
          } catch (Exception e) {
            assert false: "These algorithms should always be available";
          }
          try {
            rsaSig.setPubKey(next.keycomponents.get(1), next.keycomponents.get(2));
          } catch (Exception e) {
            log.log(Level.WARNING, "Failed to set public key", e);
            continue;
          }
          sig = rsaSig;
        }
        boolean verified = false;
        try {
          sig.update(pkToken.getSign());
          try {
            verified = sig.verify(nextSig);
          } catch (Throwable t) { // verify failed
          }
          if (verified) {
            found = true;
            break;
          }
        } catch (Exception e) {
          log.log(Level.WARNING, "Failed to verify signature", e);
          continue;
        }
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
