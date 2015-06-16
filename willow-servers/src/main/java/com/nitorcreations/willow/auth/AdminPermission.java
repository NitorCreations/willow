package com.nitorcreations.willow.auth;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.DomainPermission;

public class AdminPermission extends DomainPermission {

  private static final long serialVersionUID = -715220420504565084L;

  public AdminPermission() {
    super("admin");
  }
  @Override
  public boolean implies(Permission p) {
    return true;
  }


}
